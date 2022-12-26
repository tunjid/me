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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

/**
 *
 * Class that allows moving navigation content between composables
 */
internal data class MoveableNav(
    val mainRouteId: String = Route404.id,
    val sideRouteId: String? = null,
    val moveKind: MoveKind = MoveKind.None,
    val containerOneAndRoute: ContainerAndRoute = ContainerAndRoute(container = ContentContainer.One),
    val containerTwoAndRoute: ContainerAndRoute = ContainerAndRoute(container = ContentContainer.Two, route = Route403)
)

internal val MoveableNav.mainContainer: ContentContainer
    get() = when (mainRouteId) {
        containerOneAndRoute.route.id -> containerOneAndRoute.container
        containerTwoAndRoute.route.id -> containerTwoAndRoute.container
        else -> throw IllegalArgumentException()
    }

internal val MoveableNav.sideContainer: ContentContainer?
    get() = when (sideRouteId) {
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
    MainToNavRail, NavRailToMain, None
}

internal fun NavStateHolder.moveableNav(): Flow<MoveableNav> =
    state
        .map { it.current to it.navRail }
        .distinctUntilChanged()
        .scan(
            MoveableNav(
                mainRouteId = state.value.current.id,
                containerOneAndRoute = ContainerAndRoute(
                    route = state.value.current,
                    container = ContentContainer.One
                ),
            )
        ) { oldSwap, (mainRoute, navRailRoute) ->
            val moveableNavContext = MoveableNavContext(
                slotOneAndRoute = oldSwap.containerOneAndRoute,
                slotTwoAndRoute = oldSwap.containerTwoAndRoute
            )
            moveableNavContext.moveToFreeSpot(
                incoming = navRailRoute,
                outgoing = mainRoute,
            )
            moveableNavContext.moveToFreeSpot(
                incoming = mainRoute,
                outgoing = navRailRoute,
            )
            MoveableNav(
                mainRouteId = mainRoute.id,
                sideRouteId = navRailRoute?.id,
                moveKind = when {
                    oldSwap.mainRouteId == navRailRoute?.id -> MoveKind.MainToNavRail
                    mainRoute.id == oldSwap.sideRouteId -> MoveKind.NavRailToMain
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

private fun MoveableNavContext.moveToFreeSpot(incoming: AppRoute?, outgoing: AppRoute?) =
    when (incoming) {
        null -> Unit
        slotOneAndRoute.route -> slotOneAndRoute = slotOneAndRoute.copy(route = incoming)
        slotTwoAndRoute.route -> slotTwoAndRoute = slotTwoAndRoute.copy(route = incoming)
        else -> when (outgoing) {
            slotOneAndRoute.route -> slotTwoAndRoute = slotTwoAndRoute.copy(route = incoming)
            slotTwoAndRoute.route -> slotOneAndRoute = slotOneAndRoute.copy(route = incoming)
            else -> slotOneAndRoute = slotOneAndRoute.copy(route = incoming)
        }
    }