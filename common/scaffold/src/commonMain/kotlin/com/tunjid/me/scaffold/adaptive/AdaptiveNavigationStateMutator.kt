// See YouTrack: KTIJ-18375
@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")
package com.tunjid.me.scaffold.adaptive

import com.tunjid.me.scaffold.adaptive.Adaptive.Adaptation.Companion.PrimaryToTransient
import com.tunjid.me.scaffold.adaptive.Adaptive.Container.Primary
import com.tunjid.me.scaffold.adaptive.Adaptive.Container.Secondary
import com.tunjid.me.scaffold.adaptive.Adaptive.Container.TransientPrimary
import com.tunjid.me.scaffold.globalui.BackStatus
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.WindowSizeClass
import com.tunjid.me.scaffold.globalui.slices.routeContainerState
import com.tunjid.me.scaffold.navigation.AdaptiveRoute
import com.tunjid.me.scaffold.navigation.UnknownRoute
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.Order
import com.tunjid.treenav.current
import com.tunjid.treenav.pop
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.traverse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.withIndex

internal sealed class Action {
    data class RouteExitStart(val routeId: String) : Action()

    data class RouteExitEnd(val routeId: String) : Action()
}

internal fun CoroutineScope.adaptiveNavigationStateMutator(
    routeParser: RouteParser<AdaptiveRoute>,
    navStateFlow: StateFlow<MultiStackNav>,
    uiStateFlow: StateFlow<UiState>,
) = actionStateFlowProducer<Action, Adaptive.NavigationState>(
    initialState = Adaptive.NavigationState.Initial,
    started = SharingStarted.WhileSubscribed(3_000),
    mutationFlows = listOf(
        adaptiveNavigationStateMutations(
            routeParser = routeParser,
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

/**
 * Adapts the [MultiStackNav] navigation state to one best optimized for display in the current
 * UI window configuration.
 */
private fun adaptiveNavigationStateMutations(
    routeParser: RouteParser<AdaptiveRoute>,
    navStateFlow: StateFlow<MultiStackNav>,
    uiStateFlow: StateFlow<UiState>
): Flow<Mutation<Adaptive.NavigationState>> = combine(
    flow = navStateFlow.withIndex(),
    flow2 = uiStateFlow.distinctUntilChangedBy {
        listOf(it.backStatus, it.windowSizeClass, it.routeContainerState)
    },
) { (navId, multiStackNav), uiState ->
    routeParser.adaptiveNavigationState(
        multiStackNav = multiStackNav,
        uiState = uiState,
        navId = navId
    )
}
    .distinctUntilChanged()
    .scan(
        initial = Adaptive.NavigationState.Initial.adaptTo(
            new = routeParser.adaptiveNavigationState(
                multiStackNav = navStateFlow.value,
                uiState = uiStateFlow.value,
                navId = -1,
            )
        ),
        operation = Adaptive.NavigationState::adaptTo
    )
    .mapToMutation { newState ->
        // Replace the entire state except the knowledge of routes animating in and out
        newState.copy(routeIdsAnimatingOut = routeIdsAnimatingOut)
    }

private fun RouteParser<AdaptiveRoute>.adaptiveNavigationState(
    multiStackNav: MultiStackNav,
    uiState: UiState,
    navId: Int
): Adaptive.NavigationState {
    // If there is a back preview in progress, show the back primary route in the
    // primary container
    val primaryRoute = multiStackNav.primaryRouteOnBackPress.takeIf {
        uiState.backStatus.previewState == BackStatus.PreviewState.Previewing
    } ?: multiStackNav.primaryRoute

    // Parse the secondary route from the primary route
    val secondaryRoute = primaryRoute.secondaryRoute?.id?.let(this::parse)

    return Adaptive.NavigationState(
        navId = navId,
        containersToRoutes = mapOf(
            Primary to primaryRoute,
            Secondary to secondaryRoute.takeIf { route ->
                route?.id != primaryRoute.id
                        && uiState.windowSizeClass > WindowSizeClass.COMPACT
            },
            TransientPrimary to multiStackNav.primaryRoute.takeIf { route ->
                uiState.backStatus.previewState == BackStatus.PreviewState.Previewing
                        && route.id != primaryRoute.id
                        && route.id != secondaryRoute?.id
            },
        ),
        windowSizeClass = uiState.windowSizeClass,
        adaptation = when (uiState.backStatus.previewState) {
            BackStatus.PreviewState.Previewing -> Adaptive.Adaptation.Swap(
                from = Primary,
                to = TransientPrimary,
            )

            // Tentative, decide downstream
            else -> Adaptive.Adaptation.Change(
                previewState = uiState.backStatus.previewState
            )
        },
        backStackIds = mutableSetOf<String>().apply {
            multiStackNav.traverse(Order.DepthFirst) { add(it.id) }
        },
        // Tentative, decide downstream
        routeIdsAnimatingOut = emptySet(),
        // Tentative, decide downstream
        routeIdsToAdaptiveSlots = emptyMap(),
        // Tentative, decide downstream
        previousContainersToRoutes = emptyMap(),
        routeContainerPositionalState = uiState.routeContainerState,
    )
}

/**
 * A method that adapts changes in navigation to different containers while allowing for them
 * to be animated easily.
 */
private fun Adaptive.NavigationState.adaptTo(
    new: Adaptive.NavigationState,
): Adaptive.NavigationState {
    val old = this
    val newAdaptation = when (new.adaptation) {
        // Moved from primary container to transient, keep as is
        PrimaryToTransient -> new.adaptation
        else -> when {
            old.containersToRoutes[Primary]?.id == new.containersToRoutes[Secondary]?.id -> {
                Adaptive.Adaptation.PrimaryToSecondary
            }

            old.containersToRoutes[Secondary]?.id == new.containersToRoutes[Primary]?.id -> {
                Adaptive.Adaptation.SecondaryToPrimary
            }

            old.adaptation == PrimaryToTransient
                    && new.containersToRoutes[TransientPrimary] == null -> when (new.navId) {
                old.navId -> when {
                    new.adaptation is Adaptive.Adaptation.Change
                            && new.adaptation.previewState
                            == BackStatus.PreviewState.CancelledAfterPreview
                    -> Adaptive.Adaptation.TransientToPrimary
                    // Wait for the actual navigation state to change
                    else -> old.adaptation
                }
                // Navigation has changed, adapt
                else -> Adaptive.Adaptation.TransientDismissal
            }

            else -> when (new.navId) {
                // Wait for the actual navigation state to change
                old.navId -> old.adaptation
                else -> new.adaptation
            }
        }
    }

    return when (newAdaptation) {
        // In a change, each container should keep its slot from the previous state.
        // This allows the AnimatedContent transition run on the route id
        is Adaptive.Adaptation.Change -> {
            val updatedRouteIdsToAdaptiveSlots = mutableMapOf<String?, Adaptive.Slot>()
            // For routes in all containers, look up its previous slot
            // If that slot is null, find the first slot that hasn't been taken up yet
            // otherwise reuse its existing slot
            for (lookup in AdaptiveRouteInContainerLookups) {
                val currentRoute = lookup(new) ?: continue
                val previousRoute = lookup(old)
                val slot = when (val oldSlot = old.routeIdsToAdaptiveSlots[previousRoute?.id]) {
                    null -> old.routeIdsToAdaptiveSlots.entries.first { entry ->
                        !updatedRouteIdsToAdaptiveSlots.containsValue(entry.value)
                    }.value

                    else -> oldSlot
                }
                updatedRouteIdsToAdaptiveSlots[currentRoute.id] = slot
            }
            while (updatedRouteIdsToAdaptiveSlots.size < Adaptive.Slot.entries.size) {
                val unUsedSlot = Adaptive.Slot.entries.first { slot ->
                    !updatedRouteIdsToAdaptiveSlots.containsValue(slot)
                }
                updatedRouteIdsToAdaptiveSlots[unUsedSlot.name] = unUsedSlot
            }
            new.copy(
                adaptation = newAdaptation,
                previousContainersToRoutes = Adaptive.Container.entries.associateWith(
                    valueSelector = old::routeFor
                ),
                routeIdsToAdaptiveSlots = updatedRouteIdsToAdaptiveSlots
            )
        }

        // In a swap, preserve the existing slot for a route, however find new routes coming in
        // an assign unoccupied slots to them.
        is Adaptive.Adaptation.Swap -> when {
            // No change appreciable change in routes, keep things as is
            AdaptiveRouteInContainerLookups.map(new::run)
                    == AdaptiveRouteInContainerLookups.map(old::run)
                    && old.adaptation == newAdaptation -> new.copy(
                adaptation = newAdaptation,
                previousContainersToRoutes = old.previousContainersToRoutes,
                routeIdsToAdaptiveSlots = old.routeIdsToAdaptiveSlots
            )

            else -> when (newAdaptation) {
                Adaptive.Adaptation.TransientDismissal -> {
                    val currentRouteIds = AdaptiveRouteInContainerLookups
                        .map { new.let(it)?.id }
                        .toSet()

                    val unusedSlots = old.routeIdsToAdaptiveSlots
                        .filterKeys { !currentRouteIds.contains(it) }
                        .values
                        .toMutableList()

                    new.copy(
                        adaptation = newAdaptation,
                        previousContainersToRoutes = Adaptive.Container.entries.associateWith(
                            valueSelector = old::routeFor
                        ),
                        routeIdsToAdaptiveSlots = AdaptiveRouteInContainerLookups
                            .mapNotNull { lookup ->
                                val route = lookup(new) ?: return@mapNotNull null
                                route.id to when (
                                    val existingSlot = old.routeIdsToAdaptiveSlots[route.id]
                                ) {
                                    null -> unusedSlots.removeAt(index = 0)
                                    else -> existingSlot
                                }
                            }.toMap()
                    )
                }

                else -> {
                    val swappedRoute = old.routeFor(newAdaptation.from)
                    val swappedSlot = old.slotFor(newAdaptation.from)
                        ?: Adaptive.Slot.entries.firstOrNull { old.routeFor(it) == null }
                        ?: throw IllegalArgumentException("Attempted move from a null slot")

                    val freeSlots = Adaptive.Slot.entries
                        .filterNot(swappedSlot::equals)
                        .toMutableList()

                    new.copy(
                        adaptation = newAdaptation,
                        previousContainersToRoutes = Adaptive.Container.entries.associateWith(
                            valueSelector = old::routeFor
                        ),
                        routeIdsToAdaptiveSlots = AdaptiveRouteInContainerLookups
                            .mapNotNull { lookup ->
                                val route = lookup(new) ?: return@mapNotNull null
                                route.id to if (route.id == swappedRoute?.id) swappedSlot
                                else freeSlots.removeAt(index = 0)
                            }
                            .toMap()
                    )
                }
            }
        }
    }
}

private fun Flow<Action.RouteExitStart>.routeExitStartMutations(): Flow<Mutation<Adaptive.NavigationState>> =
    mapToMutation { exitStart ->
        copy(routeIdsAnimatingOut = routeIdsAnimatingOut + exitStart.routeId)
    }

private fun Flow<Action.RouteExitEnd>.routeExitEndMutations(): Flow<Mutation<Adaptive.NavigationState>> =
    mapToMutation { exitEnd ->
        copy(routeIdsAnimatingOut = routeIdsAnimatingOut - exitEnd.routeId).prune()
    }

/**
 * Checks if any of the new routes coming in has any conflicts with those animating out.
 */
private fun Adaptive.NavigationState.hasConflictingRoutes() =
    routeIdsAnimatingOut.contains(routeFor(Primary)?.id)
            || routeFor(Secondary)?.id?.let(routeIdsAnimatingOut::contains) == true
            || routeFor(TransientPrimary)?.id?.let(routeIdsAnimatingOut::contains) == true

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

private val AdaptiveRouteInContainerLookups: List<Adaptive.NavigationState.() -> AdaptiveRoute?> =
    Adaptive.Container.entries.map { container ->
        { routeFor(container) }
    }


private val MultiStackNav.primaryRoute: AdaptiveRoute get() = current as? AdaptiveRoute ?: UnknownRoute()

private val MultiStackNav.primaryRouteOnBackPress: AdaptiveRoute? get() = pop().current as? AdaptiveRoute