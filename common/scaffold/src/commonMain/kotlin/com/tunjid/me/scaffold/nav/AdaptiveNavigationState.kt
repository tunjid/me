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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
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
    val primaryRoute: AppRoute = UnknownRoute(AdaptiveContainer.entries.first().name),
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
     * A mapping of route ids to the containers they are currently in.
     */
    val routeIdsToContainers: Map<String, AdaptiveContainer> = AdaptiveContainer.entries
        .associateBy(AdaptiveContainer::name),
)

internal val AdaptiveNavigationState.primaryContainer: AdaptiveContainer
    get() = routeIdsToContainers.getValue(primaryRoute.id)

internal val AdaptiveNavigationState.secondaryContainer: AdaptiveContainer?
    get() = routeIdsToContainers[secondaryRoute?.id]

@Suppress("unused")
internal enum class AdaptiveContainer {
    One, Two
}

internal operator fun AdaptiveNavigationState.get(container: AdaptiveContainer): AppRoute =
    when (container) {
        routeIdsToContainers[primaryRoute.id] -> primaryRoute
        routeIdsToContainers[secondaryRoute?.id] -> secondaryRoute
        routeIdsToContainers[transientPrimaryBackRoute?.id] -> transientPrimaryBackRoute
        else -> null
    } ?: UnknownRoute("--")

internal enum class MoveKind {
    PrimaryToSecondary, SecondaryToPrimary, None
}

internal fun StateFlow<NavState>.adaptiveNavigationState(): Flow<AdaptiveNavigationState> =
    this
        .map { navState ->
            Triple(
                navState.primaryRoute,
                navState.secondaryRoute,
                navState.predictiveBackRoute,
            )
        }
        .distinctUntilChanged()
        .scan(
            with(AdaptiveNavigationState()) {
                copy(
                    primaryRoute = value.primaryRoute,
                    secondaryRoute = value.secondaryRoute,
                    routeIdsToContainers = routeIdsToContainers.include(
                        listOfNotNull(
                            value.primaryRoute,
                            value.secondaryRoute
                        )
                    )
                )
            }
        ) { previous, (primaryRoute, secondaryRoute, predictiveBackRoute) ->
            val routesToContainers = previous.routeIdsToContainers
                .include(listOfNotNull(primaryRoute, secondaryRoute))

            AdaptiveNavigationState(
                primaryRoute = primaryRoute,
                secondaryRoute = secondaryRoute,
                transientPrimaryBackRoute = predictiveBackRoute,
                moveKind = when {
                    previous.primaryRoute.id == secondaryRoute?.id -> MoveKind.PrimaryToSecondary
                    primaryRoute.id == previous.secondaryRoute?.id -> MoveKind.SecondaryToPrimary
                    else -> MoveKind.None
                },
                routeIdsToContainers = routesToContainers,
            )
        }

private fun Map<String, AdaptiveContainer>.include(
    incoming: List<AppRoute>,
): Map<String, AdaptiveContainer> {
    val routesToAdd = incoming.filterNot { contains(it.id) }
    val relevantRouteIds = incoming.map { it.id }.toSet()
    val sortedByIrrelevance = entries.sortedBy { relevantRouteIds.contains(it.key) }

    val replacedKeyValuePairs = List(sortedByIrrelevance.size) { index ->
        val sortedEntry = sortedByIrrelevance[index]
        (routesToAdd.getOrNull(index)?.id ?: sortedEntry.key) to sortedEntry.value
    }
    return replacedKeyValuePairs.toMap()
}