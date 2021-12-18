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

package com.tunjid.me.nav

import androidx.compose.runtime.Composable

interface Route<T> {
    val id: String

    @Composable
    fun Render()
}

data class StackNav(
    val name: String,
    val routes: List<Route<*>> = listOf()
)

fun StackNav.push(route: Route<*>) = copy(
    routes = routes + route
)

fun StackNav.pop() = copy(
    routes = routes.dropLast(1)
)

data class MultiStackNav(
    val currentIndex: Int = -1,
    val stacks: List<StackNav> = listOf()
)

fun MultiStackNav.push(route: Route<*>) = copy(
    stacks = stacks.mapIndexed { index, stack ->
        if (index == currentIndex) stack.push(route = route)
        else stack
    }
)

fun MultiStackNav.pop() = copy(
    stacks = stacks.mapIndexed { index, stack ->
        if (index == currentIndex) stack.pop()
        else stack
    }
)

val MultiStackNav.routes get() = stacks.map(StackNav::routes).flatten()

val MultiStackNav.current get() = stacks.getOrNull(currentIndex)?.routes?.last()