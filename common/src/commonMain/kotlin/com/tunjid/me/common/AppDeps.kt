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

package com.tunjid.me.common

import androidx.compose.runtime.staticCompositionLocalOf
import com.tunjid.me.common.data.Api
import com.tunjid.me.common.data.archive.ArchiveRepository
import com.tunjid.me.common.data.archive.RestArchiveRepository
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.globalui.globalUiMutator
import com.tunjid.me.common.nav.MultiStackNav
import com.tunjid.me.common.nav.Route
import com.tunjid.me.common.nav.navMutator
import com.tunjid.me.common.nav.routes
import com.tunjid.me.common.ui.archive.ArchiveRoute
import com.tunjid.me.common.ui.archive.archiveMutator
import com.tunjid.me.common.ui.archivedetail.ArchiveDetailRoute
import com.tunjid.me.common.ui.archivedetail.archiveDetailMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import io.ktor.client.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

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

    val api: Api = Api(HttpClient {
        install(JsonFeature) {
            accept(ContentType.Application.Json, ContentType.Text.Html)
            serializer = KotlinxSerializer(json = Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            level = LogLevel.INFO
            logger = object : Logger {
                override fun log(message: String) = println("Logger Ktor => $message")
            }
        }
    })

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

val LocalAppDependencies = staticCompositionLocalOf {
    stubAppDeps()
}

fun stubAppDeps(
    nav: MultiStackNav = MultiStackNav(),
    globalUI: UiState = UiState()
): AppDeps = object : AppDeps {
    override val navMutator: Mutator<Mutation<MultiStackNav>, StateFlow<MultiStackNav>> =
        object : Mutator<Mutation<MultiStackNav>, StateFlow<MultiStackNav>> {
            override val accept: (Mutation<MultiStackNav>) -> Unit = {}
            override val state: StateFlow<MultiStackNav> = MutableStateFlow(nav)
        }
    override val globalUiMutator: Mutator<Mutation<UiState>, StateFlow<UiState>> =
        object : Mutator<Mutation<UiState>, StateFlow<UiState>> {
            override val accept: (Mutation<UiState>) -> Unit = {}
            override val state: StateFlow<UiState> = MutableStateFlow(globalUI)
        }

    override fun <T> routeDependencies(route: Route<T>): T =
        TODO("Not yet implemented")
}
