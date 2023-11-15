package com.tunjid.me.scaffold.globalui.adaptive

import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.WindowSizeClass
import com.tunjid.me.scaffold.globalui.adaptive.Adaptive.Adaptation.Change.unaffectedContainers
import com.tunjid.me.scaffold.globalui.isPreviewing
import com.tunjid.me.scaffold.globalui.slices.routeContainerState
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.nav.NavState
import com.tunjid.me.scaffold.nav.primaryRoute
import com.tunjid.me.scaffold.nav.primaryRouteOnBackPress
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.Order
import com.tunjid.treenav.flatten
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.withIndex

internal sealed class Action {
    data class RouteExitStart(val id: String) : Action()

    data class RouteExitEnd(val id: String) : Action()

}

internal fun CoroutineScope.adaptiveNavigationStateMutator(
    navStateFlow: StateFlow<NavState>,
    uiStateFlow: StateFlow<UiState>,
) = actionStateFlowProducer<Action, Adaptive.NavigationState>(
    initialState = Adaptive.NavigationState.Initial,
    started = SharingStarted.WhileSubscribed(3_000),
    mutationFlows = listOf(
        adaptiveNavigationStateMutations(
            navStateFlow = navStateFlow,
            uiStateFlow = uiStateFlow
        )
    ),
    actionTransform = { actions ->
        actions.toMutationStream {
            when (val action = type()) {
                is Action.RouteExitStart -> action.flow.routeExitStartMutations()
                is Action.RouteExitEnd -> action.flow.routeExitEndMutations()
            }
        }
    },
    stateTransform = { adaptiveNavFlow ->
        adaptiveNavFlow.filterNot(Adaptive.NavigationState::hasConflictingRoutes)
    }
)

private fun adaptiveNavigationStateMutations(
    navStateFlow: StateFlow<NavState>,
    uiStateFlow: StateFlow<UiState>
): Flow<Mutation<Adaptive.NavigationState>> = combine(
    flow = navStateFlow.withIndex(),
    flow2 = uiStateFlow.distinctUntilChangedBy {
        listOf(it.backStatus, it.windowSizeClass, it.routeContainerState)
    },
) { (navId, navState), uiState ->
    // If there is a back preview in progress, show the back primary route in the
    // primary container
    val visiblePrimaryRoute = navState.primaryRouteOnBackPress.takeIf {
        uiState.backStatus.isPreviewing
    } ?: navState.primaryRoute

    Adaptive.NavigationState(
        navId = navId,
        primaryRoute = visiblePrimaryRoute,
        secondaryRoute = navState.secondaryRoute.takeIf { route ->
            route?.id != visiblePrimaryRoute.id
                    && uiState.windowSizeClass > WindowSizeClass.COMPACT
        },
        transientPrimaryRoute = navState.primaryRoute.takeIf { route ->
            uiState.backStatus.isPreviewing
                    && route.id != visiblePrimaryRoute.id
                    && route.id != navState.secondaryRoute?.id
        },
        windowSizeClass = uiState.windowSizeClass,
        adaptation = when {
            uiState.backStatus.isPreviewing -> Adaptive.Adaptation.Swap(
                from = Adaptive.Container.Primary,
                to = Adaptive.Container.TransientPrimary,
            )

            // Tentative, decide downstream
            else -> Adaptive.Adaptation.Change
        },
        backStackIds = navState.mainNav.flatten(Order.DepthFirst).map { it.id }.toSet(),
        // Tentative, decide downstream
        routeIdsAnimatingOut = emptySet(),
        // Tentative, decide downstream
        routeIdsToAdaptiveSlots = emptyMap(),
        // Tentative, decide downstream
        previousContainersToRoutes = emptyMap(),
        routeContainerPositionalState = uiState.routeContainerState,
    )
}
    .distinctUntilChanged()
    .scan(
        initial = Adaptive.NavigationState.Initial,
        operation = Adaptive.NavigationState::adaptTo
    )
    .mapToMutation { newState ->
        // Replace the entire state except the knowledge of routes animating in and out
        newState.copy(routeIdsAnimatingOut = routeIdsAnimatingOut)
    }

/**
 * A method that adapts changes in navigation to different containers while allowing for them
 * to be animated easily.
 */
private fun Adaptive.NavigationState.adaptTo(
    current: Adaptive.NavigationState,
): Adaptive.NavigationState {
    val newAdaptation = when (current.adaptation) {
        // Moved from primary container to transient, keep as is
        Adaptive.Adaptation.PrimaryToTransient -> current.adaptation
        else -> when {
            primaryRoute.id == current.secondaryRoute?.id -> Adaptive.Adaptation.PrimaryToSecondary
            current.primaryRoute.id == secondaryRoute?.id -> Adaptive.Adaptation.SecondaryToPrimary
            adaptation == Adaptive.Adaptation.PrimaryToTransient
                    && current.transientPrimaryRoute == null -> when (current.navId) {
                // Wait for the actual navigation state to change
                navId -> adaptation
                // Navigation has changed, adapt
                else -> when (transientPrimaryRoute?.id) {
                    current.primaryRoute.id -> Adaptive.Adaptation.TransientToPrimary
                    else -> Adaptive.Adaptation.TransientDismissal
                }
            }

            else -> when (current.navId) {
                // Wait for the actual navigation state to change
                navId -> adaptation
                else -> Adaptive.Adaptation.Change
            }
        }
    }

    return when (newAdaptation) {
        // In a change, each container should keep its slot from the previous state.
        // This allows the AnimatedContent transition run on the route id
        Adaptive.Adaptation.Change -> {
            val updatedRouteIdsToAdaptiveSlots = mutableMapOf<String, Adaptive.Slot>()
            // For routes in all containers, look up its previous slot
            // If that slot is null, find the first slot that hasn't been taken up yet
            // otherwise reuse its existing slot
            for (lookup in AdaptiveRouteInContainerLookups) {
                val currentRoute = lookup(current) ?: continue
                val previousRoute = lookup(this)
                val slot = when (val previousSlot = routeIdsToAdaptiveSlots[previousRoute?.id]) {
                    null -> routeIdsToAdaptiveSlots.entries.first { entry ->
                        !updatedRouteIdsToAdaptiveSlots.containsValue(entry.value)
                    }.value

                    else -> previousSlot
                }
                updatedRouteIdsToAdaptiveSlots[currentRoute.id] = slot
            }
            // TODO: Remove stale route ids after they complete their transition
            current.copy(
                adaptation = newAdaptation,
                previousContainersToRoutes = Adaptive.Container.entries.associateWith(::routeFor),
                routeIdsToAdaptiveSlots = routeIdsToAdaptiveSlots + updatedRouteIdsToAdaptiveSlots
            )
        }

        // In a swap, preserve the existing slot for a route, however find new routes coming in
        // an assign unoccupied slots to them.
        is Adaptive.Adaptation.Swap -> when {
            // No change appreciable change in routes, keep things as is
            AdaptiveRouteInContainerLookups.map(current::run)
                    == AdaptiveRouteInContainerLookups.map(this::run)
                    && adaptation == newAdaptation -> current.copy(
                adaptation = newAdaptation,
                previousContainersToRoutes = previousContainersToRoutes,
                routeIdsToAdaptiveSlots = routeIdsToAdaptiveSlots
            )

            else -> {
                val fromSlot = this.slotFor(newAdaptation.from)
                val excludedSlots = newAdaptation.unaffectedContainers()
                    .map(::slotFor)
                    .plus(fromSlot)
                    .toSet()

                val vacatedSlot = Adaptive.Slot.entries.first {
                    !excludedSlots.contains(it)
                }
                current.copy(
                    adaptation = newAdaptation,
                    previousContainersToRoutes = Adaptive.Container.entries.associateWith(::routeFor),
                    routeIdsToAdaptiveSlots = when (val newRoute =
                        current.routeFor(newAdaptation.from)) {
                        null -> routeIdsToAdaptiveSlots
                        else -> routeIdsToAdaptiveSlots - routeFor(vacatedSlot)?.id + Pair(
                            first = newRoute.id,
                            second = vacatedSlot
                        )
                    }
                )
            }
        }
    }
}

private fun Flow<Action.RouteExitStart>.routeExitStartMutations(): Flow<Mutation<Adaptive.NavigationState>> =
    mapToMutation { exitStart ->
        copy(routeIdsAnimatingOut = routeIdsAnimatingOut + exitStart.id)
    }

private fun Flow<Action.RouteExitEnd>.routeExitEndMutations(): Flow<Mutation<Adaptive.NavigationState>> =
    mapToMutation { exitEnd ->
        copy(routeIdsAnimatingOut = routeIdsAnimatingOut - exitEnd.id).prune()
    }

/**
 * Checks if any of the new routes coming in has any conflicts with those animating out.
 */
private fun Adaptive.NavigationState.hasConflictingRoutes() =
    routeIdsAnimatingOut.contains(primaryRoute.id)
            || secondaryRoute?.id?.let(routeIdsAnimatingOut::contains) == true
            || transientPrimaryRoute?.id?.let(routeIdsAnimatingOut::contains) == true

/**
 * Trims unneeded metadata from the [Adaptive.NavigationState]
 */
private fun Adaptive.NavigationState.prune(): Adaptive.NavigationState = copy(
    routeIdsToAdaptiveSlots = routeIdsToAdaptiveSlots.filter { (routeId) ->
        if (routeId == null) return@filter false
        backStackIds.contains(routeId)
                || routeIdsAnimatingOut.contains(routeId)
                || previousContainersToRoutes.values.map { it?.id }.toSet().contains(routeId)
    },
    previousContainersToRoutes = previousContainersToRoutes.filter { (_, route) ->
        if (route == null) return@filter false
        backStackIds.contains(route.id)
                || routeIdsAnimatingOut.contains(route.id)
                || previousContainersToRoutes.values.map { it?.id }.toSet().contains(route.id)
    }
)


private val AdaptiveRouteInContainerLookups: List<(Adaptive.NavigationState) -> AppRoute?> = listOf(
    Adaptive.NavigationState::primaryRoute,
    Adaptive.NavigationState::secondaryRoute,
    Adaptive.NavigationState::transientPrimaryRoute,
)