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

import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.core.utilities.toBytes
import com.tunjid.me.feature.RouteServiceLocator
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.ScaffoldComponent
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.nav.NavState
import com.tunjid.me.scaffold.nav.Route404
import com.tunjid.me.scaffold.nav.removedRoutes
import com.tunjid.me.scaffold.savedstate.SavedState
import com.tunjid.me.scaffold.savedstate.SavedStateRepository
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.Order
import com.tunjid.treenav.flatten
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

@Inject
class RouteMutatorFactory(
    appScope: CoroutineScope,
    byteSerializer: ByteSerializer,
    navStateStream: StateFlow<NavState>,
    savedStateRepository: SavedStateRepository,
    private val allScreenStateHolders: Map<String, ScreenStateHolderCreator>
) : RouteServiceLocator {
    private val routeMutatorCache = mutableMapOf<AppRoute, ScopeHolder>()

    init {
        appScope.launch {
            navStateStream
                .map { it.mainNav }
                .removedRoutes()
                .collect { removedRoutes ->
                    removedRoutes.forEach { route ->
                        println("Cleared ${route::class.simpleName}")
                        val holder = routeMutatorCache.remove(route)
                        holder?.scope?.cancel()
                    }
                }
        }
        appScope.launch {
            navStateStream
                .map { it.mainNav }
                .map { it.toSavedState(byteSerializer) }
                .collectLatest(savedStateRepository::saveState)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> locate(route: AppRoute): T =
        routeMutatorCache.getOrPut(route) {
            val routeScope = CoroutineScope(
                SupervisorJob() + Dispatchers.Main.immediate
            )
            ScopeHolder(
                scope = routeScope,
                mutator = when (route) {
                    is Route404 -> Route404
                    else -> allScreenStateHolders
                        .getValue(route::class.simpleName!!)
                        .invoke(routeScope, route)
                }

            )

        }.mutator as T

    private fun MultiStackNav.toSavedState(
        byteSerializer: ByteSerializer
    ) = SavedState(
        isEmpty = false,
        activeNav = currentIndex,
        navigation = stacks.fold(listOf()) { listOfLists, stackNav ->
            listOfLists.plus(
                element = stackNav.routes
                    .filterIsInstance<AppRoute>()
                    .fold(listOf()) { stackList, route ->
                        stackList + route.id
                    }
            )
        },
        routeStates = flatten(order = Order.BreadthFirst)
            .filterIsInstance<AppRoute>()
            .fold(mutableMapOf()) { map, route ->
                val mutator = locate<Any>(route)
                val state = (mutator as? ActionStateProducer<*, *>)?.state ?: return@fold map
                val serializable = (state as? StateFlow<*>)?.value ?: return@fold map
                if (serializable is ByteSerializable) map[route.id] =
                    byteSerializer.toBytes(serializable)
                map
            })
}

private data class ScopeHolder(
    val scope: CoroutineScope,
    val mutator: Any
)
