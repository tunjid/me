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

package com.tunjid.me

import androidx.compose.runtime.staticCompositionLocalOf
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.tunjid.me.data.Api
import com.tunjid.me.data.archive.ArchiveRepository
import com.tunjid.me.data.archive.RestArchiveRepository
import com.tunjid.me.globalui.UiState
import com.tunjid.me.globalui.globalUiMutator
import com.tunjid.me.nav.MultiStackNav
import com.tunjid.me.nav.Route
import com.tunjid.me.nav.navMutator
import com.tunjid.me.nav.routes
import com.tunjid.me.ui.archive.ArchiveRoute
import com.tunjid.me.ui.archive.archiveMutator
import com.tunjid.me.ui.archivedetail.ArchiveDetailRoute
import com.tunjid.me.ui.archivedetail.archiveDetailMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit


interface AppDeps {
    val navMutator: Mutator<Mutation<MultiStackNav>, StateFlow<MultiStackNav>>
    val globalUiMutator: Mutator<Mutation<UiState>, StateFlow<UiState>>
    fun <T> routeDependencies(route: Route<T>): T
}

private data class ScopeHolder(
    val scope: CoroutineScope,
    val mutator: Any
)

fun createAppDependencies(scope: CoroutineScope) = object : AppDeps {
    val routeMutatorFactory = mutableMapOf<Route<*>, ScopeHolder>()
    private val json = Json { ignoreUnknownKeys = true }

    val api: Api = Retrofit.Builder()
        .baseUrl("https://www.tunjid.com/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .client(
            OkHttpClient()
                .newBuilder()
                .addInterceptor(HttpLoggingInterceptor().apply { setLevel(HttpLoggingInterceptor.Level.BODY) })
                .build()
        )
        .build()
        .create(Api::class.java)
    val archiveRepository: ArchiveRepository = RestArchiveRepository(api = api)

    override val navMutator: Mutator<Mutation<MultiStackNav>, StateFlow<MultiStackNav>> =
        navMutator(scope = scope)

    override val globalUiMutator: Mutator<Mutation<UiState>, StateFlow<UiState>> =
        globalUiMutator(scope = scope)

    init {
        scope.launch {
            navMutator.state
                .map { it.routes }
                .distinctUntilChanged()
                .scan(listOf<Route<*>>() to listOf<Route<*>>()) { pair, newRoutes ->
                    pair.copy(first = pair.second, second = newRoutes)
                }
                .distinctUntilChanged()
                .collect { (oldRoutes, newRoutes) ->
                    oldRoutes.minus(newRoutes.toSet()).forEach { route ->
                        val holder = routeMutatorFactory.remove(route)
                        holder?.scope?.cancel()
                    }
                }
        }
    }

    override fun <T> routeDependencies(route: Route<T>): T = when (route) {
        is ArchiveRoute -> routeMutatorFactory.getOrPut(route) {
            val routeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            ScopeHolder(
                scope = routeScope,
                mutator = archiveMutator(
                    scope = routeScope,
                    route = route,
                    repo = archiveRepository
                )
            )
        }
        is ArchiveDetailRoute -> routeMutatorFactory.getOrPut(route) {
            val routeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            ScopeHolder(
                scope = routeScope,
                mutator = archiveDetailMutator(
                    scope = routeScope,
                    archive = route.archive,
                    repo = archiveRepository
                )
            )
        }
        else -> throw IllegalArgumentException("Unknown route")
    }.mutator as T
}

val LocalAppDependencies = staticCompositionLocalOf<AppDeps> {
    object : AppDeps {
        override val navMutator: Mutator<Mutation<MultiStackNav>, StateFlow<MultiStackNav>>
            get() = TODO("Stub!")
        override val globalUiMutator: Mutator<Mutation<UiState>, StateFlow<UiState>>
            get() = TODO("Stub!")

        override fun <T> routeDependencies(route: Route<T>): T =
            TODO("Not yet implemented")
    }
}
