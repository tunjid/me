/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.me.scaffold.nav

import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.WindowSizeClass
import com.tunjid.me.scaffold.globalui.isPreviewing
import com.tunjid.me.scaffold.nav.MoveKind.Change.unaffectedContainers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

internal enum class AdaptiveContainer {
    Primary, Secondary, TransientPrimary
}

@Suppress("unused")
internal enum class AdaptiveContainerSlot {
    One, Two, Three
}

/**
 * Data structure for managing navigation as it adapts to various layout configurations
 */
internal data class AdaptiveNavigationState(
    /**
     * The route in the primary navigation container
     */
    val primaryRoute: AppRoute,
    /**
     * The route in the secondary navigation container
     */
    val secondaryRoute: AppRoute?,
    /**
     * The route that will show up in the primary navigation container after back is pressed.
     * This is used to preview the incoming route in the primary navigation container after a
     * back press. If a back destination does not need to be previewed, it will be null.
     */
    val transientPrimaryRoute: AppRoute?,
    /**
     * Describes moves between the primary and secondary navigation containers.
     */
    val moveKind: MoveKind,
    /**
     * A mapping of route ids to the adaptive slots they are currently in.
     */
    val routeIdsToAdaptiveSlots: Map<String?, AdaptiveContainerSlot>,
    /**
     * The window size class of the current screen configuration
     */
    val windowSizeClass: WindowSizeClass,
) {
    companion object {
        internal val Initial = AdaptiveNavigationState(
            primaryRoute = UnknownRoute(AdaptiveContainerSlot.One.name),
            secondaryRoute = null,
            transientPrimaryRoute = null,
            moveKind = MoveKind.Change,
            windowSizeClass = WindowSizeClass.COMPACT,
            routeIdsToAdaptiveSlots = AdaptiveContainerSlot.entries
                .associateBy(AdaptiveContainerSlot::name),
        )
    }
}

/**
 * Information about content in an [AdaptiveContainerSlot]
 */
internal data class AdaptiveSlotMetadata(
    val route: AppRoute?,
    val slot: AdaptiveContainerSlot?,
    val container: AdaptiveContainer?,
    val moveKind: MoveKind,
)

internal fun AdaptiveNavigationState.metadataFor(
    slot: AdaptiveContainerSlot
): AdaptiveSlotMetadata {
    val route = routeFor(slot)
    return AdaptiveSlotMetadata(
        slot = slot,
        route = route,
        container = route?.let(::containerFor),
        moveKind = moveKind
    )
}

internal fun AdaptiveNavigationState.slotFor(
    container: AdaptiveContainer
): AdaptiveContainerSlot? = when (container) {
    AdaptiveContainer.Primary -> routeIdsToAdaptiveSlots[primaryRoute.id]
    AdaptiveContainer.Secondary -> routeIdsToAdaptiveSlots[secondaryRoute?.id]
    AdaptiveContainer.TransientPrimary -> routeIdsToAdaptiveSlots[transientPrimaryRoute?.id]
}

internal fun AdaptiveNavigationState.containerFor(
    route: AppRoute
): AdaptiveContainer? = when (route.id) {
    primaryRoute.id -> AdaptiveContainer.Primary
    secondaryRoute?.id -> AdaptiveContainer.Secondary
    transientPrimaryRoute?.id -> AdaptiveContainer.TransientPrimary
    else -> null
}

internal fun AdaptiveNavigationState.routeFor(
    slot: AdaptiveContainerSlot
): AppRoute? = when (slot) {
    routeIdsToAdaptiveSlots[primaryRoute.id] -> primaryRoute
    routeIdsToAdaptiveSlots[secondaryRoute?.id] -> secondaryRoute
    routeIdsToAdaptiveSlots[transientPrimaryRoute?.id] -> transientPrimaryRoute
    else -> null
}

internal fun AdaptiveNavigationState.routeFor(
    container: AdaptiveContainer
): AppRoute? = when (container) {
    AdaptiveContainer.Primary -> primaryRoute
    AdaptiveContainer.Secondary -> secondaryRoute
    AdaptiveContainer.TransientPrimary -> transientPrimaryRoute
}

internal sealed class MoveKind {

    /**
     * Routes were changed in containers
     */
    data object Change : MoveKind()

    /**
     * Routes were swapped in between containers
     */
    data class Swap(
        val from: AdaptiveContainer,
        val to: AdaptiveContainer,
    ) : MoveKind()

    fun Swap.unaffectedContainers() = AdaptiveContainer.entries - setOf(from, to)

    fun Swap.affects(container: AdaptiveContainer?) = from == container || to == container

    companion object {
        val PrimaryToSecondary = Swap(
            from = AdaptiveContainer.Primary,
            to = AdaptiveContainer.Secondary
        )

        val SecondaryToPrimary = Swap(
            from = AdaptiveContainer.Secondary,
            to = AdaptiveContainer.Primary
        )

        val PrimaryToTransient = Swap(
            from = AdaptiveContainer.Primary,
            to = AdaptiveContainer.TransientPrimary
        )

        val TransientToPrimary = Swap(
            from = AdaptiveContainer.TransientPrimary,
            to = AdaptiveContainer.Primary
        )
    }
}

internal fun StateFlow<NavState>.adaptiveNavigationState(
    uiState: StateFlow<UiState>
): Flow<AdaptiveNavigationState> =
    combine(uiState, ::Pair)
        .map { (navState, uiState) ->
            // If there is a back preview in progress, show the back primary route in the
            // primary container
            val visiblePrimaryRoute = navState.primaryRouteOnBackPress.takeIf {
                uiState.backStatus.isPreviewing
            } ?: navState.primaryRoute

            AdaptiveNavigationState(
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
                moveKind = when {
                    uiState.backStatus.isPreviewing -> MoveKind.Swap(
                        from = AdaptiveContainer.Primary,
                        to = AdaptiveContainer.TransientPrimary,
                    )

                    else -> MoveKind.Change
                },
                routeIdsToAdaptiveSlots = emptyMap()
            )
        }
        .distinctUntilChanged()
        .scan(
            initial = AdaptiveNavigationState.Initial,
            operation = AdaptiveNavigationState::adaptTo
        )

private fun AdaptiveNavigationState.adaptTo(
    current: AdaptiveNavigationState,
): AdaptiveNavigationState {
    val moveKind = when (current.moveKind) {
        MoveKind.PrimaryToTransient -> current.moveKind
        else -> when {
            primaryRoute.id == current.secondaryRoute?.id -> MoveKind.PrimaryToSecondary
            current.primaryRoute.id == secondaryRoute?.id -> MoveKind.SecondaryToPrimary
            moveKind == MoveKind.PrimaryToTransient
                    && current.transientPrimaryRoute == null -> MoveKind.TransientToPrimary

            else -> MoveKind.Change
        }
    }

    return when (moveKind) {
        // In a change, each container should keep its slot from the previous state.
        // This allows the AnimatedContent transition run on the route id
        MoveKind.Change -> {
            val updatedRouteIdsToAdaptiveSlots = mutableMapOf<String, AdaptiveContainerSlot>()
            // Going from the primary route downwards, look up its previous slot
            // If that slot is null, find the first slot that hasn't been taken up yet
            // otherwise reuse its existing slot
            val routeLookups = listOf(
                AdaptiveNavigationState::primaryRoute,
                AdaptiveNavigationState::secondaryRoute,
                AdaptiveNavigationState::transientPrimaryRoute,
            )
            for (lookup in routeLookups) {
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
            current.copy(
                moveKind = moveKind,
                routeIdsToAdaptiveSlots = routeIdsToAdaptiveSlots + updatedRouteIdsToAdaptiveSlots
            )
        }

        // In a swap, preserve the existing slot for a route, however find new routes coming in
        // an assign unoccupied slots to them.
        is MoveKind.Swap -> {
            val fromSlot = this.slotFor(moveKind.from)
            val excludedSlots = moveKind.unaffectedContainers()
                .map(::slotFor)
                .plus(fromSlot)
                .toSet()

            val vacatedSlot = AdaptiveContainerSlot.entries.first {
                !excludedSlots.contains(it)
            }
            current.copy(
                moveKind = moveKind,
                routeIdsToAdaptiveSlots = when (val newRoute = current.routeFor(moveKind.from)) {
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
