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
import com.tunjid.me.common.nav.AppRoute
import com.tunjid.me.common.nav.navMutator
import com.tunjid.me.common.ui.archive.ArchiveRoute
import com.tunjid.me.common.ui.archive.archiveMutator
import com.tunjid.me.common.ui.archivedetail.ArchiveDetailRoute
import com.tunjid.me.common.ui.archivedetail.archiveDetailMutator
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.Order
import com.tunjid.treenav.flatten
import io.ktor.client.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

interface AppDependencies {
    val appMutator: AppMutator
    fun <T> routeDependencies(route: AppRoute<T>): T
}

private data class ScopeHolder(
    val scope: CoroutineScope,
    val mutator: Any
)

fun createAppDependencies(
    scope: CoroutineScope,
    initialUiState: UiState = UiState()
) = object : AppDependencies {
    val routeMutatorFactory = mutableMapOf<AppRoute<*>, ScopeHolder>()

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

    override val appMutator: AppMutator = appMutator(
        scope = scope,
        navMutator = navMutator(scope = scope),
        globalUiMutator = globalUiMutator(scope = scope, initialState = initialUiState)
    )

    init {
        scope.launch {
            appMutator.state
                .map { it.nav.flatten(Order.BreadthFirst).filterIsInstance<AppRoute<*>>() }
                .distinctUntilChanged()
                .scan(listOf<AppRoute<*>>() to listOf<AppRoute<*>>()) { pair, newRoutes ->
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

    override fun <T> routeDependencies(route: AppRoute<T>): T = when (route) {
        is ArchiveRoute -> routeMutatorFactory.getOrPut(route) {
            val routeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            ScopeHolder(
                scope = routeScope,
                mutator = archiveMutator(
                    scope = routeScope,
                    route = route,
                    repo = archiveRepository,
                    appMutator = appMutator,
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
                    repo = archiveRepository,
                    appMutator = appMutator,
                )
            )
        }
        else -> throw IllegalArgumentException("Unknown route")
    }.mutator as T
}

val LocalAppDependencies = staticCompositionLocalOf {
    stubAppDependencies()
}

fun stubAppDependencies(
    nav: MultiStackNav = MultiStackNav(name = "App"),
    globalUI: UiState = UiState()
): AppDependencies = object : AppDependencies {

    override val appMutator: AppMutator = AppState(
        ui = globalUI,
        nav = nav
    ).asAppMutator

    override fun <T> routeDependencies(route: AppRoute<T>): T =
        TODO("Not yet implemented")
}
