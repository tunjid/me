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

import com.tunjid.me.data.di.DataComponent
import com.tunjid.me.feature.Feature
import com.tunjid.me.feature.RouteServiceLocator
import com.tunjid.me.feature.find
import com.tunjid.me.scaffold.di.ScaffoldComponent
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.nav.removedRoutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class RouteMutatorFactory(
    appScope: CoroutineScope,
    private val features: List<Feature<*, *>>,
    private val scaffoldComponent: ScaffoldComponent,
    private val dataComponent: DataComponent,
) : RouteServiceLocator {
    private val routeMutatorCache = mutableMapOf<AppRoute, ScopeHolder>()

    init {
        appScope.launch {
            scaffoldComponent
                .navStateStream
                .removedRoutes()
                .collect { removedRoutes ->
                    removedRoutes.forEach { route ->
                        println("Cleared ${route::class.simpleName}")
                        val holder = routeMutatorCache.remove(route)
                        holder?.scope?.cancel()
                    }
                }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> locate(route: AppRoute): T =
        routeMutatorCache.getOrPut(route) {
            val routeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            ScopeHolder(
                scope = routeScope,
                mutator = features.find(route)
                    .mutator(
                        scope = routeScope,
                        route = route,
                        scaffoldComponent = scaffoldComponent,
                        dataComponent = dataComponent
                    )
            )

        }.mutator as T
}

private data class ScopeHolder(
    val scope: CoroutineScope,
    val mutator: Any
)
