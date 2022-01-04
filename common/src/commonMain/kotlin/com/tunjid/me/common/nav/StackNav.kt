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

package com.tunjid.me.common.nav

/**
 * An immutable navigation [Node] whose children are represented with a stack like data structure
 */
data class StackNav(
    val name: String,
    val routes: List<Route> = listOf()
) : Node {
    override val id: String get() = name
    override val children: List<Node> get() = routes
}

/**
 * Swaps the top of the stack with the specified [Route]
 */
fun StackNav.swap(route: Route) = if (routes.lastOrNull() == route) this else copy(
    routes = routes.dropLast(1) + route
)

/**
 * Pushes the [route] unto the top of the navigation stack
 */
fun StackNav.push(route: Route) = if (routes.lastOrNull() == route) this else copy(
    routes = routes + route
)

/**
 * Pops the top route off if the stack is larger than 1, otherwise, it no ops
 * TODO: Make the no op behavior optional
 */
fun StackNav.pop() = if (routes.size == 1) this else copy(
    routes = routes.dropLast(1)
)

/**
 * Indicates if there's a [Route] available to pop up to
 */
val StackNav.canGoUp get() = routes.size > 1
