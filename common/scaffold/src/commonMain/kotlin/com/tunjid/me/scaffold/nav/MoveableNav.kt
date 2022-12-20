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

internal data class MoveableNav(
    val mainRouteId: String = Route404.id,
    val navRouteId: String? = null,
    val slotOneAndRoute: SlotAndRoute = SlotAndRoute(slot = SwapSlot.One),
    val slotTwoAndRoute: SlotAndRoute = SlotAndRoute(slot = SwapSlot.Two, route = Route403)
)

internal val MoveableNav.mainSlot
    get() = when (mainRouteId) {
        slotOneAndRoute.route.id -> slotOneAndRoute.slot
        slotTwoAndRoute.route.id -> slotTwoAndRoute.slot
        else -> throw IllegalArgumentException()
    }

internal val MoveableNav.navSlot
    get() = when (navRouteId) {
        slotOneAndRoute.route.id -> slotOneAndRoute.slot
        slotTwoAndRoute.route.id -> slotTwoAndRoute.slot
        else -> null
    }

internal data class SlotAndRoute(
    val route: AppRoute = Route404,
    val slot: SwapSlot,
)

internal enum class SwapSlot {
    One, Two
}

internal fun NavStateHolder.moveableNav(): Flow<MoveableNav> =
    state
        .map { (it.current as? AppRoute ?: Route404) to it.navRail }
        .distinctUntilChanged()
        .scan(
            MoveableNav(
                mainRouteId = (state.value.current as? AppRoute ?: Route404).id,
                slotOneAndRoute = SlotAndRoute(
                    route = state.value.current as? AppRoute ?: Route404,
                    slot = SwapSlot.One
                ),
            )
        ) { oldSwap, (mainRoute, navRailRoute) ->
            val moveableNavContext = MoveableNavContext(
                slotOneAndRoute = oldSwap.slotOneAndRoute,
                slotTwoAndRoute = oldSwap.slotTwoAndRoute
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
                navRouteId = navRailRoute?.id,
                slotOneAndRoute = moveableNavContext.slotOneAndRoute,
                slotTwoAndRoute = moveableNavContext.slotTwoAndRoute,
            )
        }

private class MoveableNavContext(
    var slotOneAndRoute: SlotAndRoute,
    var slotTwoAndRoute: SlotAndRoute
)

private fun MoveableNavContext.moveToFreeSpot(incoming: AppRoute?, outgoing: AppRoute?) {
    println("incoming: ${incoming?.id}; outgoing: ${outgoing?.id}")
    return when (incoming) {
        null -> Unit
        slotOneAndRoute.route -> slotOneAndRoute = slotOneAndRoute.copy(route = incoming)
        slotTwoAndRoute.route -> slotTwoAndRoute = slotTwoAndRoute.copy(route = incoming)
        else -> when (outgoing) {
            slotOneAndRoute.route -> slotTwoAndRoute = slotTwoAndRoute.copy(route = incoming)
            slotTwoAndRoute.route -> slotOneAndRoute = slotOneAndRoute.copy(route = incoming)
            else -> slotOneAndRoute = slotOneAndRoute.copy(route = incoming)
        }
    }
}