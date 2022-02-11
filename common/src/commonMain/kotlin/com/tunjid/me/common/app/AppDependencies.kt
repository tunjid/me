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

import androidx.compose.runtime.staticCompositionLocalOf
import com.tunjid.me.common.data.AppDatabase
import com.tunjid.me.common.data.ByteSerializable
import com.tunjid.me.common.data.ByteSerializer
import com.tunjid.me.common.data.DelegatingByteSerializer
import com.tunjid.me.common.data.fromBytes
import com.tunjid.me.common.data.local.ArchiveDao
import com.tunjid.me.common.data.local.SessionCookieDao
import com.tunjid.me.common.data.local.SqlArchiveDao
import com.tunjid.me.common.data.local.SqlSessionCookieDao
import com.tunjid.me.common.data.local.databaseDispatcher
import com.tunjid.me.common.data.model.ArchiveKind
import com.tunjid.me.common.data.network.Api
import com.tunjid.me.common.data.network.ApiUrl
import com.tunjid.me.common.data.network.NetworkMonitor
import com.tunjid.me.common.data.network.exponentialBackoff
import com.tunjid.me.common.data.repository.ArchiveRepository
import com.tunjid.me.common.data.repository.AuthRepository
import com.tunjid.me.common.data.repository.ReactiveArchiveRepository
import com.tunjid.me.common.data.repository.SessionCookieAuthRepository
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.globalui.globalUiMutator
import com.tunjid.me.common.nav.AppRoute
import com.tunjid.me.common.nav.ByteSerializableRoute
import com.tunjid.me.common.nav.navMutator
import com.tunjid.me.common.nav.removedRoutes
import com.tunjid.me.common.ui.archive.ArchiveRoute
import com.tunjid.me.common.ui.archive.archiveMutator
import com.tunjid.me.common.ui.archivedetail.ArchiveDetailRoute
import com.tunjid.me.common.ui.archivedetail.archiveDetailMutator
import com.tunjid.me.common.ui.auth.SignInRoute
import com.tunjid.me.common.ui.auth.signInMutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

interface AppDependencies {
    val appDatabase: AppDatabase
    val appMutator: AppMutator
    val networkMonitor: NetworkMonitor
    val byteSerializer: ByteSerializer
    val archiveRepository: ArchiveRepository
    val authRepository: AuthRepository
    val archiveDao: ArchiveDao
    val sessionCookieDao: SessionCookieDao
    fun <T> routeDependencies(route: AppRoute<T>): T
}

private data class ScopeHolder(
    val scope: CoroutineScope,
    val mutator: Any
)

fun createAppDependencies(
    appScope: CoroutineScope,
    initialUiState: UiState = UiState(),
    database: AppDatabase,
    networkMonitor: NetworkMonitor,
): AppDependencies = AppModule(
    appDatabase = database,
    networkMonitor = networkMonitor,
    appScope = appScope,
    initialUiState = initialUiState
)

/**
 * Poor man's dependency injection module
 */
private class AppModule(
    override val appDatabase: AppDatabase,
    override val networkMonitor: NetworkMonitor,
    appScope: CoroutineScope,
    initialUiState: UiState
) : AppDependencies {

    val routeMutatorFactory = mutableMapOf<AppRoute<*>, ScopeHolder>()

    override val archiveDao = SqlArchiveDao(
        database = appDatabase,
        dispatcher = databaseDispatcher(),
    )

    override val sessionCookieDao: SessionCookieDao = SqlSessionCookieDao(
        database = appDatabase,
        dispatcher = databaseDispatcher(),
    )

    val api: Api = Api(
        sessionCookieDao = sessionCookieDao
    )

    override val archiveRepository: ArchiveRepository = ReactiveArchiveRepository(
        api = api,
        appScope = appScope,
        networkMonitor = networkMonitor,
        dao = archiveDao
    )

    override val authRepository: AuthRepository =
        SessionCookieAuthRepository(
            api = api,
            dao = sessionCookieDao
        )

    override val appMutator: AppMutator = appMutator(
        scope = appScope,
        navMutator = navMutator(scope = appScope),
        globalUiMutator = globalUiMutator(scope = appScope, initialState = initialUiState)
    )

    // TODO: Pass this as an argument
    override val byteSerializer: ByteSerializer = DelegatingByteSerializer(
        format = Cbor {
            serializersModule = SerializersModule {
                polymorphic(ByteSerializableRoute::class) {
                    subclass(ArchiveRoute::class)
                    subclass(ArchiveDetailRoute::class)
                    subclass(SignInRoute::class)
                }
                polymorphic(ByteSerializable::class) {
                    subclass(com.tunjid.me.common.ui.archive.State::class)
                    subclass(com.tunjid.me.common.ui.archivedetail.State::class)
                    subclass(com.tunjid.me.common.ui.auth.State::class)
                }
            }
        }
    )

    init {
        appScope.launch {
            appMutator.state
                .map { it.nav }
                .removedRoutes()
                .collect { removedRoutes ->
                    removedRoutes.forEach { route ->
                        val holder = routeMutatorFactory.remove(route)
                        holder?.scope?.cancel()
                    }
                }
        }

        appScope.launch {
            com.tunjid.me.common.data.network.modelEvents(
                url = "$ApiUrl/",
                dispatcher = databaseDispatcher()
            )
                .mapNotNull { event ->
                    val kind = when (event.collection) {
                        ArchiveKind.Articles.type -> ArchiveKind.Articles
                        ArchiveKind.Talks.type -> ArchiveKind.Talks
                        ArchiveKind.Projects.type -> ArchiveKind.Projects
                        else -> null
                    } ?: return@mapNotNull null

                    exponentialBackoff(
                        initialDelay = 1_000,
                        maxDelay = 20_000,
                        times = 5,
                        default = null
                    ) { api.fetchArchive(kind = kind, id = event.id) }
                }
                .collect {
                    archiveDao.saveArchive(it)
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
                    initialState = route.restoredState(),
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
                    initialState = route.restoredState(),
                    route = route,
                    repo = archiveRepository,
                    appMutator = appMutator,
                )
            )
        }
        is SignInRoute -> routeMutatorFactory.getOrPut(route) {
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

    private inline fun <reified T : ByteSerializable> AppRoute<*>.restoredState(): T? {
        return try {
            // TODO: Figure out why this throws
            val serialized = appMutator.state.value.routeIdsToSerializedStates[id]
            serialized?.let(byteSerializer::fromBytes)
        } catch (e: Exception) {
            null
        }
    }
}

val LocalAppDependencies = staticCompositionLocalOf {
    stubAppDependencies()
}
