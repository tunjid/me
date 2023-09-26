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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

/**
 * Data structure for managing navigation as it adapts to various layout configurations
 */
internal data class AdaptiveNavigationState(
    /**
     * The route in the primary navigation container
     */
    val primaryRoute: AppRoute = UnknownRoute(AdaptiveContainerSlot.entries.first().name),
    /**
     * The route in the secondary navigation container
     */
    val secondaryRoute: AppRoute? = null,
    /**
     * The route that will show up in the primary navigation container after back is pressed.
     * This is used to preview the incoming route in the primary navigation container after a
     * back press. If a back destination does not need to be previewed, it will be null.
     */
    val transientPrimaryBackRoute: AppRoute? = null,
    /**
     * Describes moves between the primary and secondary navigation containers.
     */
    val moveKind: MoveKind = MoveKind.None,
    /**
     * A mapping of route ids to the adaptive slots they are currently in.
     */
    val routeIdsToAdaptiveSlots: Map<String, AdaptiveContainerSlot> = AdaptiveContainerSlot.entries
        .associateBy(AdaptiveContainerSlot::name),
)

internal val AdaptiveNavigationState.primaryContainerSlot: AdaptiveContainerSlot
    get() = routeIdsToAdaptiveSlots.getValue(primaryRoute.id)

internal operator fun AdaptiveNavigationState.get(
    container: AdaptiveContainer
): AdaptiveContainerSlot? = when (container) {
    AdaptiveContainer.Primary -> primaryContainerSlot
    AdaptiveContainer.Secondary -> routeIdsToAdaptiveSlots[secondaryRoute?.id]
    AdaptiveContainer.TransientPrimary -> routeIdsToAdaptiveSlots[transientPrimaryBackRoute?.id]
}

internal enum class AdaptiveContainer {
    Primary, Secondary, TransientPrimary
}

@Suppress("unused")
internal enum class AdaptiveContainerSlot {
    One, Two, Three
}

internal operator fun AdaptiveNavigationState.get(container: AdaptiveContainerSlot): AppRoute =
    when (container) {
        routeIdsToAdaptiveSlots[primaryRoute.id] -> primaryRoute
        routeIdsToAdaptiveSlots[secondaryRoute?.id] -> secondaryRoute
        routeIdsToAdaptiveSlots[transientPrimaryBackRoute?.id] -> transientPrimaryBackRoute
        else -> null
    } ?: UnknownRoute("--")

internal enum class MoveKind {
    PrimaryToSecondary, SecondaryToPrimary, None
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
                transientPrimaryBackRoute = navState.primaryRoute.takeIf { route ->
                    uiState.backStatus.isPreviewing
                            && route.id != visiblePrimaryRoute.id
                            && route.id != navState.secondaryRoute?.id
                },
            )
        }
        .distinctUntilChanged()
        .scan(
            with(AdaptiveNavigationState()) {
                copy(
                    primaryRoute = value.primaryRoute,
                    secondaryRoute = value.secondaryRoute,
                    routeIdsToAdaptiveSlots = routeIdsToAdaptiveSlots.include(
                        listOfNotNull(
                            value.primaryRoute,
                            value.secondaryRoute,
                            value.primaryRouteOnBackPress.takeIf { route ->
                                route?.id != value.secondaryRoute?.id
                                        && route?.id != value.primaryRoute.id
                            },
                        )
                    )
                )
            }
        ) { previous, current ->
            current.copy(
                moveKind = when {
                    previous.primaryRoute.id == current.secondaryRoute?.id -> MoveKind.PrimaryToSecondary
                    current.primaryRoute.id == previous.secondaryRoute?.id -> MoveKind.SecondaryToPrimary
                    else -> MoveKind.None
                },
                routeIdsToAdaptiveSlots = previous.routeIdsToAdaptiveSlots.include(
                    listOfNotNull(
                        current.transientPrimaryBackRoute,
                        current.primaryRoute,
                        current.secondaryRoute
                    )
                ),
            )
        }

private fun Map<String, AdaptiveContainerSlot>.include(
    incoming: List<AppRoute>,
): Map<String, AdaptiveContainerSlot> {
    val routesToAdd = incoming.filterNot { contains(it.id) }
    val relevantRouteIds = incoming.map { it.id }.toSet()
    val sortedByIrrelevance = entries.sortedBy { relevantRouteIds.contains(it.key) }

    val replacedKeyValuePairs = List(sortedByIrrelevance.size) { index ->
        val sortedEntry = sortedByIrrelevance[index]
        (routesToAdd.getOrNull(index)?.id ?: sortedEntry.key) to sortedEntry.value
    }
    return replacedKeyValuePairs.toMap()
}