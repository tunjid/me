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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.canGoUp
import com.tunjid.treenav.current
import com.tunjid.treenav.minus
import com.tunjid.treenav.switch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.serialization.Serializable

const val NavName = "App"

typealias NavMutator = Mutator<Mutation<MultiStackNav>, StateFlow<MultiStackNav>>

interface AppRoute<T : Mutator<*, *>> : ByteSerializableRoute {
    @Composable
    fun Render()

    /**
     * Defines what route to show in the nav rail along side this route
     */
    fun navRailRoute(nav: MultiStackNav): AppRoute<*>? = null
}

@Serializable
object Route404 : AppRoute<Mutator<Unit, Unit>> {
    override val id: String
        get() = "404"

    @Composable
    override fun Render() {
        Box {
            Text(
                modifier = Modifier
                    .padding(),
                text = "404"
            )
        }
    }
}

data class NavItem(
    val name: String,
    val icon: ImageVector,
    val index: Int,
    val selected: Boolean
)

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

private fun MultiStackNav.popToRoot(indexToPop: Int) = copy(
    stacks = stacks.mapIndexed { index: Int, stackNav: StackNav ->
        if (index == indexToPop) stackNav.popToRoot()
        else stackNav
    }
)

private fun StackNav.popToRoot() = copy(
    routes = routes.take(1)
)

fun navMutator(
    scope: CoroutineScope,
    startNav: MultiStackNav,
): NavMutator =
    stateFlowMutator(
        scope = scope,
        initialState = startNav,
        actionTransform = { it },
    )

fun Flow<MultiStackNav>.removedRoutes(): Flow<List<AppRoute<*>>> =
    distinctUntilChanged()
        .scan(initial = listOf(emptyNav, emptyNav)) { list, newNav ->
            (list + newNav).takeLast(2)
        }
        .map { (prevNav: MultiStackNav, currentNav: MultiStackNav) ->
            (prevNav - currentNav).filterIsInstance<AppRoute<*>>()
        }

private val emptyNav = MultiStackNav(
    name = NavName,
    currentIndex = 0,
    stacks = listOf(),
)