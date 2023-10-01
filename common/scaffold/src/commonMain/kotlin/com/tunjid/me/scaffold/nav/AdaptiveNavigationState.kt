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
    /**
     * The window size class of the current screen configuration
     */
    val windowSizeClass: WindowSizeClass = WindowSizeClass.COMPACT,
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

internal operator fun AdaptiveNavigationState.get(
    route: AppRoute
): AdaptiveContainer? = when (route.id) {
    primaryRoute.id -> AdaptiveContainer.Primary
    secondaryRoute?.id -> AdaptiveContainer.Secondary
    transientPrimaryBackRoute?.id -> AdaptiveContainer.TransientPrimary
    else -> null
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
                windowSizeClass = uiState.windowSizeClass,
            )
        }
        .distinctUntilChanged()
        .scan(
            with(AdaptiveNavigationState()) {
                copy(
                    primaryRoute = value.primaryRoute,
                    secondaryRoute = value.secondaryRoute,
                    routeIdsToAdaptiveSlots = placeRoutesInSlots(
                        existingRoutesToSlots = routeIdsToAdaptiveSlots,
                        incomingContainersToRoutes = listOfNotNull(
                            value.primaryRoute.placeIn(AdaptiveContainer.Primary),
                            value.secondaryRoute.placeIn(AdaptiveContainer.Secondary),
                            value.primaryRouteOnBackPress
                                .takeIf { route ->
                                    route?.id != value.secondaryRoute?.id
                                            && route?.id != value.primaryRoute.id
                                }
                                .placeIn(AdaptiveContainer.TransientPrimary),
                        ).toMap()
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
                routeIdsToAdaptiveSlots = placeRoutesInSlots(
                    existingRoutesToSlots = previous.routeIdsToAdaptiveSlots,
                    incomingContainersToRoutes = listOfNotNull(
                        current.primaryRoute.placeIn(AdaptiveContainer.Primary),
                        current.secondaryRoute.placeIn(AdaptiveContainer.Secondary),
                        current.transientPrimaryBackRoute.placeIn(AdaptiveContainer.TransientPrimary),
                    ).toMap()
                ),
            )
        }

private fun AppRoute?.placeIn(container: AdaptiveContainer) =
    if (this == null) null
    else container to this

private fun placeRoutesInSlots(
    existingRoutesToSlots: Map<String, AdaptiveContainerSlot>,
    incomingContainersToRoutes: Map<AdaptiveContainer, AppRoute>,
): Map<String, AdaptiveContainerSlot> {
    // A set of ids that will be guaranteed a place in the slots
    val relevantRouteIds = incomingContainersToRoutes
        .map { it.value.id }
        .toSet()

    // Sort existing routes by irrelevance; routes to be ejected will be first
    val sortedByIrrelevance = existingRoutesToSlots.entries
        .sortedBy { relevantRouteIds.contains(it.key) }

    // Find routes to display that are not already in slots
    val routesToAdd = incomingContainersToRoutes.entries
        .filterNot { existingRoutesToSlots.contains(it.value.id) }
        .sortedBy { it.key }

    // Replace irrelevant routes with incoming routes that were not previously cached
    val replacedKeyValuePairs = List(sortedByIrrelevance.size) { index ->
        val sortedEntry = sortedByIrrelevance[index]
        (routesToAdd.getOrNull(index)?.value?.id ?: sortedEntry.key) to sortedEntry.value
    }
    return replacedKeyValuePairs.toMap()
}