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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.me.common.data.archive.icon
import com.tunjid.me.common.data.model.ArchiveKind
import com.tunjid.me.common.data.model.ArchiveQuery
import com.tunjid.me.common.ui.archivelist.ArchiveListRoute
import com.tunjid.me.common.ui.settings.SettingsRoute
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.canGoUp
import com.tunjid.treenav.current
import com.tunjid.treenav.minus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.serialization.Serializable

const val NavName = "App"

typealias NavMutator = Mutator<Mutation<MultiStackNav>, StateFlow<MultiStackNav>>

interface AppRoute<T> : ByteSerializableRoute {
    @Composable
    fun Render()

    /**
     * Defines what route to show in the nav rail along side this route
     */
    fun navRailRoute(nav: MultiStackNav): AppRoute<*>? = null
}

@Serializable
object Route404 : AppRoute<Unit> {
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

fun navMutator(scope: CoroutineScope): NavMutator =
    stateFlowMutator(
        scope = scope,
        initialState = startNav,
        actionTransform = { it },
    )

fun Flow<MultiStackNav>.removedRoutes(): Flow<List<AppRoute<*>>> =
    distinctUntilChanged()
        .scan(initial = startNav to listOf<AppRoute<*>>()) { pair, newNav ->
            pair.copy(
                first = newNav,
                second = (newNav - pair.first).filterIsInstance<AppRoute<*>>()
            )
        }
        .map { it.second }

private val startNav = MultiStackNav(
    name = NavName,
    currentIndex = 0,
    stacks = ArchiveKind.values().map { kind ->
        StackNav(
            name = kind.name,
            routes = listOf(ArchiveListRoute(query = ArchiveQuery(kind = kind)))
        )
    } + StackNav(
        name = "Settings",
        routes = listOf(SettingsRoute)
    )
)
