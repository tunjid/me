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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.treenav.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

val MultiStackNav.canGoUp get() = stacks.getOrNull(currentIndex)?.canGoUp == true

val MultiStackNav.navItems
    get() = stacks
        .map { it.name }
        .mapIndexed { index, name ->
            val kind = ArchiveKind.values().firstOrNull { it.name == name }
            NavItem(
                name = name,
                icon = kind?.icon ?: Icons.Default.Settings,
                index = index,
                selected = currentIndex == index,
            )
        }

val MultiStackNav.navRailRoute: AppRoute<*>?
    get() = when (val current = current) {
        is AppRoute<*> -> current.navRailRoute(this)
        else -> null
    }

fun MultiStackNav.navItemSelected(item: NavItem) =
    if (item.selected) popToRoot(indexToPop = item.index)
    else switch(toIndex = item.index)

/**
 * Route diff between consecutive emissions of [MultiStackNav]
 */
fun Flow<MultiStackNav>.removedRoutes(): Flow<List<AppRoute<*>>> =
    distinctUntilChanged()
        .scan(initial = listOf(emptyNav, emptyNav)) { list, newNav ->
            (list + newNav).takeLast(2)
        }
        .map { (prevNav: MultiStackNav, currentNav: MultiStackNav) ->
            (prevNav - currentNav).filterIsInstance<AppRoute<*>>()
        }

private fun MultiStackNav.popToRoot(indexToPop: Int) = copy(
    stacks = stacks.mapIndexed { index: Int, stackNav: StackNav ->
        if (index == indexToPop) stackNav.popToRoot()
        else stackNav
    }
)

private fun StackNav.popToRoot() = copy(
    routes = routes.take(1)
)

private val emptyNav = MultiStackNav(
    name = NavName,
    currentIndex = 0,
    stacks = listOf(),
)
