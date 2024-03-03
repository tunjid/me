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

package com.tunjid.me.scaffold.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.canPop
import com.tunjid.treenav.minus
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.switch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

val MultiStackNav.canGoUp get() = stacks.getOrNull(currentIndex)?.canPop == true

val MultiStackNav.navItems
    get() = stacks
        .map(StackNav::name)
        .mapIndexed { index, name ->
            val kind = ArchiveKind.entries.firstOrNull {
                name.contains(it.type)
            }
            NavItem(
                name = kind?.type ?: name,
                icon = kind?.icon ?: Icons.Default.Settings,
                index = index,
                selected = currentIndex == index,
            )
        }

fun MultiStackNav.navItemSelected(item: NavItem) =
    if (item.selected) popToRoot(indexToPop = item.index)
    else switch(toIndex = item.index)

/**
 * Route diff between consecutive emissions of [MultiStackNav]
 */
fun Flow<MultiStackNav>.removedRoutes(): Flow<List<Route>> =
    distinctUntilChanged()
        .scan(initial = EmptyNavigationState to EmptyNavigationState) { navPair, newNav ->
            navPair.copy(first = navPair.second, second = newNav)
        }
        .map { (prevNav: MultiStackNav, currentNav: MultiStackNav) ->
            (prevNav - currentNav).filterIsInstance<Route>()
        }

private fun MultiStackNav.popToRoot(indexToPop: Int) = copy(
    stacks = stacks.mapIndexed { index: Int, stackNav: StackNav ->
        if (index == indexToPop) stackNav.popToRoot()
        else stackNav
    }
)

private fun StackNav.popToRoot() = copy(
    children = children.take(1)
)

private val EmptyNavigationState = MultiStackNav(
    name = "App",
    currentIndex = 0,
    stacks = listOf(),
)
