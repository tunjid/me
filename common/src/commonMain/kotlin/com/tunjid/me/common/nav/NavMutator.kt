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

import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.me.common.data.archive.ArchiveKind
import com.tunjid.me.common.data.archive.ArchiveQuery
import com.tunjid.me.common.data.archive.icon
import com.tunjid.me.common.ui.archive.ArchiveRoute
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

object Paned {
    interface Control<T> : Route<T> {
        fun controls(route: Route<*>): Boolean
    }

    interface Detail {
        fun isControlledBy(route: Route<*>): Boolean
    }
}

data class NavItem(
    val name: String,
    val icon: ImageVector,
    val index: Int,
    val selected: Boolean
)

val MultiStackNav.navItems
    get() = stacks
        .map { it.name }
        .mapIndexed { index, name ->
            val kind = ArchiveKind.values().first { it.name == name }
            NavItem(
                name = name,
                icon = kind.icon,
                index = currentIndex,
                selected = currentIndex == index,
            )
        }

val MultiStackNav.railRoute: Route<*>?
    get() {
        if (currentIndex < 0) return null
        val stackRoutes = stacks.getOrNull(currentIndex)?.routes ?: return null
        val previous = stackRoutes.getOrNull(stackRoutes.lastIndex - 1) ?: return null
        val current = current ?: return null

        return if (previous is Paned.Control && previous.controls(current)) previous
        else null
    }

fun navMutator(scope: CoroutineScope): Mutator<Mutation<MultiStackNav>, StateFlow<MultiStackNav>> =
    stateFlowMutator(
        scope = scope,
        initialState = MultiStackNav(
            currentIndex = 0,
            stacks = ArchiveKind.values().map { kind ->
                StackNav(
                    name = kind.name,
                    routes = listOf(ArchiveRoute(query = ArchiveQuery(kind = kind)))
                )
            }
        ),
        transform = { it }
    )
