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

package com.tunjid.me.common.di

import androidx.compose.runtime.saveable.SaveableStateHolder
import com.tunjid.me.core.sync.changeListKey
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.core.utilities.toBytes
import com.tunjid.me.data.local.databaseDispatcher
import com.tunjid.me.data.network.ApiUrl
import com.tunjid.me.data.network.modelEvents
import com.tunjid.me.feature.ScreenStateHolderCache
import com.tunjid.me.scaffold.di.AdaptiveRouter
import com.tunjid.me.scaffold.di.SavedStateCache
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.lifecycle.LifecycleStateHolder
import com.tunjid.me.scaffold.lifecycle.monitorWhenActive
import com.tunjid.me.scaffold.navigation.NavigationStateHolder
import com.tunjid.me.scaffold.navigation.removedRoutes
import com.tunjid.me.scaffold.savedstate.SavedState
import com.tunjid.me.scaffold.savedstate.SavedStateRepository
import com.tunjid.me.scaffold.scaffold.SavedStateAdaptiveContentState
import com.tunjid.me.sync.di.Sync
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.scaffold.adaptive.StatelessRoute
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.Order
import com.tunjid.treenav.flatten
import com.tunjid.treenav.strings.PathPattern
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteTrie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import me.tatarka.inject.annotations.Inject

@Inject
class PersistedMeApp(
    appScope: CoroutineScope,
    byteSerializer: ByteSerializer,
    navStateStream: StateFlow<MultiStackNav>,
    savedStateRepository: SavedStateRepository,
    sync: Sync,
    override val navStateHolder: NavigationStateHolder,
    override val globalUiStateHolder: GlobalUiStateHolder,
    override val lifecycleStateHolder: LifecycleStateHolder,
    private val savedStateCache: SavedStateCache,
    private val allScreenStateHolders: Map<String, ScreenStateHolderCreator>,
    override val adaptiveContentStateCreator: (CoroutineScope, SaveableStateHolder) -> SavedStateAdaptiveContentState,
    override val adaptiveRouter: AdaptiveRouter,
) : MeApp {
    private val routeStateHolderCache = mutableMapOf<Route, ScopeHolder?>()

    init {
        navStateStream
            .removedRoutes()
            .onEach { removedRoutes ->
                removedRoutes.forEach { route ->
                    println("Cleared ${route::class.simpleName}")
                    val holder = routeStateHolderCache.remove(route)
                    holder?.scope?.cancel()
                }
            }
            .launchIn(appScope)

        lifecycleStateHolder.state
            .map { it.isInForeground }
            .distinctUntilChanged()
            .onStart { emit(false) }
            .flatMapLatest {
                navStateStream
                    .mapLatest { navState ->
                        navState.toSavedState(byteSerializer)
                    }
            }
            .onEach(savedStateRepository::saveState)
            .launchIn(appScope)

        modelEvents(
            url = "$ApiUrl/",
            dispatcher = databaseDispatcher()
        )
            // This is an Android concern. Remove this when this is firebase powered.
            .monitorWhenActive(lifecycleStateHolder.state)
            .map { it.model.changeListKey() }
            .onEach(sync)
            .launchIn(appScope)
    }

    override val screenStateHolderCache: ScreenStateHolderCache = object : ScreenStateHolderCache {
        private val stateHolderTrie = RouteTrie<ScreenStateHolderCreator>().apply {
            allScreenStateHolders
                .mapKeys { (template) -> PathPattern(template) }
                .forEach(::set)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T> screenStateHolderFor(route: Route): T? =
            routeStateHolderCache.getOrPut(route) {
                val stateHolderCreator = stateHolderTrie[route] ?: return@getOrPut null

                val routeScope = CoroutineScope(
                    SupervisorJob() + Dispatchers.Main.immediate
                )
                ScopeHolder(
                    scope = routeScope,
                    stateHolder = stateHolderCreator(
                        routeScope,
                        savedStateCache(route),
                        route
                    )
                )

            }?.stateHolder as? T
    }

    private fun MultiStackNav.toSavedState(
        byteSerializer: ByteSerializer,
    ) = SavedState(
        isEmpty = false,
        activeNav = currentIndex,
        navigation = stacks.fold(listOf()) { listOfLists, stackNav ->
            listOfLists.plus(
                element = stackNav.children
                    .filterIsInstance<Route>()
                    .fold(listOf()) { stackList, route ->
                        stackList + route.id
                    }
            )
        },
        routeStates = flatten(order = Order.BreadthFirst)
            .filterIsInstance<Route>()
            .fold(mutableMapOf()) { map, route ->
                val stateHolder = screenStateHolderCache.screenStateHolderFor<Any>(route)
                val state = (stateHolder as? ActionStateMutator<*, *>)?.state ?: return@fold map
                val serializable = (state as? StateFlow<*>)?.value ?: return@fold map
                if (serializable is ByteSerializable) map[route.id] =
                    byteSerializer.toBytes(serializable)
                map
            })
}

private data class ScopeHolder(
    val scope: CoroutineScope,
    val stateHolder: Any,
)

