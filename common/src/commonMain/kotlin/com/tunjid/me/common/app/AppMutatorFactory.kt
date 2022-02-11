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

package com.tunjid.me.common.app

import com.tunjid.me.common.data.ByteSerializable
import com.tunjid.me.common.data.fromBytes
import com.tunjid.me.common.nav.AppRoute
import com.tunjid.me.common.nav.removedRoutes
import com.tunjid.me.common.ui.archivedetail.ArchiveDetailRoute
import com.tunjid.me.common.ui.archivedetail.archiveDetailMutator
import com.tunjid.me.common.ui.archiveedit.ArchiveEditRoute
import com.tunjid.me.common.ui.archiveedit.archiveEditMutator
import com.tunjid.me.common.ui.archivelist.ArchiveListRoute
import com.tunjid.me.common.ui.archivelist.archiveListMutator
import com.tunjid.me.common.ui.auth.SignInRoute
import com.tunjid.me.common.ui.auth.signInMutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AppMutatorFactory(
    appScope: CoroutineScope,
    private val appDependencies: AppDependencies
) {
    private val routeMutatorCache = mutableMapOf<AppRoute<*>, ScopeHolder>()

    init {
        appScope.launch {
            appDependencies.appMutator.state
                .map { it.nav }
                .removedRoutes()
                .collect { removedRoutes ->
                    removedRoutes.forEach { route ->
                        val holder = routeMutatorCache.remove(route)
                        holder?.scope?.cancel()
                    }
                }
        }
    }

    fun <T> routeMutator(route: AppRoute<T>): T = with(appDependencies) {
        when (route) {
            is ArchiveListRoute -> routeMutatorCache.getOrPut(route) {
                val routeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
                ScopeHolder(
                    scope = routeScope,
                    mutator = archiveListMutator(
                        scope = routeScope,
                        initialState = route.restoredState(),
                        route = route,
                        archiveRepository = archiveRepository,
                        authRepository = authRepository,
                        appMutator = appMutator,
                    )
                )
            }
            is ArchiveDetailRoute -> routeMutatorCache.getOrPut(route) {
                val routeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
                ScopeHolder(
                    scope = routeScope,
                    mutator = archiveDetailMutator(
                        scope = routeScope,
                        initialState = route.restoredState(),
                        route = route,
                        archiveRepository = archiveRepository,
                        authRepository = authRepository,
                        appMutator = appMutator,
                    )
                )
            }
            is ArchiveEditRoute -> routeMutatorCache.getOrPut(route) {
                val routeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
                ScopeHolder(
                    scope = routeScope,
                    mutator = archiveEditMutator(
                        scope = routeScope,
                        initialState = route.restoredState(),
                        route = route,
                        archiveRepository = archiveRepository,
                        authRepository = authRepository,
                        appMutator = appMutator,
                    )
                )
            }
            is SignInRoute -> routeMutatorCache.getOrPut(route) {
                val routeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
                ScopeHolder(
                    scope = routeScope,
                    mutator = signInMutator(
                        scope = routeScope,
                        initialState = route.restoredState(),
                        route = route,
                        authRepository = authRepository,
                        appMutator = appMutator,
                    )
                )
            }
            else -> throw IllegalArgumentException("Unknown route")
        }.mutator as T
    }

    private inline fun <reified T : ByteSerializable> AppRoute<*>.restoredState(): T? {
        return try {
            // TODO: Figure out why this throws
            val serialized = appDependencies.appMutator.state.value.routeIdsToSerializedStates[id]
            serialized?.let(appDependencies.byteSerializer::fromBytes)
        } catch (e: Exception) {
            null
        }
    }
}

private data class ScopeHolder(
    val scope: CoroutineScope,
    val mutator: Any
)
