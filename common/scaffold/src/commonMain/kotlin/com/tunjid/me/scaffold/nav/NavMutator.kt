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

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.mutation
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.Route
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.strings.RouteParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

const val NavName = "App"

typealias NavMutator = ActionStateProducer<Mutation<MultiStackNav>, StateFlow<MultiStackNav>>

interface AppRoute : Route {
    @Composable
    fun Render()

    /**
     * Defines what route to show in the nav rail alongside this route
     */
    val navRailRoute: String?
        get() = null
}

data class NavItem(
    val name: String,
    val icon: ImageVector,
    val index: Int,
    val selected: Boolean
)

internal fun navMutator(
    scope: CoroutineScope,
    startNav: List<List<String>>,
    routeParser: RouteParser<AppRoute>,
): NavMutator {
    return scope.actionStateFlowProducer(
        initialState = routeParser.toMultiStackNav(startNav),
        actionTransform = { it },
    )
}

fun RouteParser<AppRoute>.toMultiStackNav(paths: List<List<String>>) = paths.fold(
    initial = MultiStackNav(name = "AppNav"),
    operation = { multiStackNav, routesForStack ->
        multiStackNav.copy(
            stacks = multiStackNav.stacks +
                    routesForStack.fold(
                        initial = StackNav(
                            name = routesForStack.firstOrNull() ?: "Unknown"
                        ),
                        operation = innerFold@{ stackNav, route ->
                            stackNav.copy(
                                routes = stackNav.routes + (parse(routeString = route) ?: Route404)
                            )
                        }
                    )
        )
    }
)