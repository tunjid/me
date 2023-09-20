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
 *
 * Class that allows moving navigation content between composables
 */
internal data class MoveableNav(
    val primaryRouteId: String = Route404.id,
    val secondaryRouteId: String? = null,
    val moveKind: MoveKind = MoveKind.None,
    val containerOneAndRoute: ContainerAndRoute = ContainerAndRoute(container = ContentContainer.One),
    val containerTwoAndRoute: ContainerAndRoute = ContainerAndRoute(container = ContentContainer.Two, route = Route403)
)

internal val MoveableNav.primaryContainer: ContentContainer
    get() = when (primaryRouteId) {
        containerOneAndRoute.route.id -> containerOneAndRoute.container
        containerTwoAndRoute.route.id -> containerTwoAndRoute.container
        else -> throw IllegalArgumentException()
    }

internal val MoveableNav.secondaryContainer: ContentContainer?
    get() = when (secondaryRouteId) {
        containerOneAndRoute.route.id -> containerOneAndRoute.container
        containerTwoAndRoute.route.id -> containerTwoAndRoute.container
        else -> null
    }

internal data class ContainerAndRoute(
    val route: AppRoute = Route404,
    val container: ContentContainer,
)

internal enum class ContentContainer {
    One, Two
}

internal enum class MoveKind {
    PrimaryToSecondary, SecondaryToPrimary, None
}

internal fun StateFlow<NavState>.moveableNav(): Flow<MoveableNav> =
    this
        .map { it.primaryRoute to it.secondaryRoute }
        .distinctUntilChanged()
        .scan(
            MoveableNav(
                primaryRouteId = value.primaryRoute.id,
                containerOneAndRoute = ContainerAndRoute(
                    route = value.primaryRoute,
                    container = ContentContainer.One
                ),
            )
        ) { previousMoveableNav, (primaryRoute, secondaryRoute) ->
            val moveableNavContext = MoveableNavContext(
                slotOneAndRoute = previousMoveableNav.containerOneAndRoute,
                slotTwoAndRoute = previousMoveableNav.containerTwoAndRoute
            )
            moveableNavContext.moveToFreeContainer(
                incoming = secondaryRoute,
                outgoing = primaryRoute,
            )
            moveableNavContext.moveToFreeContainer(
                incoming = primaryRoute,
                outgoing = secondaryRoute,
            )
            MoveableNav(
                primaryRouteId = primaryRoute.id,
                secondaryRouteId = secondaryRoute?.id,
                moveKind = when {
                    previousMoveableNav.primaryRouteId == secondaryRoute?.id -> MoveKind.PrimaryToSecondary
                    primaryRoute.id == previousMoveableNav.secondaryRouteId -> MoveKind.SecondaryToPrimary
                    else -> MoveKind.None
                },
                containerOneAndRoute = moveableNavContext.slotOneAndRoute,
                containerTwoAndRoute = moveableNavContext.slotTwoAndRoute,
            )
        }

private class MoveableNavContext(
    var slotOneAndRoute: ContainerAndRoute,
    var slotTwoAndRoute: ContainerAndRoute
)

private fun MoveableNavContext.moveToFreeContainer(incoming: AppRoute?, outgoing: AppRoute?) =
    when (incoming) {
        null,
        slotOneAndRoute.route,
        slotTwoAndRoute.route -> Unit
        else -> when (outgoing) {
            slotOneAndRoute.route -> slotTwoAndRoute = slotTwoAndRoute.copy(route = incoming)
            slotTwoAndRoute.route -> slotOneAndRoute = slotOneAndRoute.copy(route = incoming)
            else -> slotOneAndRoute = slotOneAndRoute.copy(route = incoming)
        }
    }