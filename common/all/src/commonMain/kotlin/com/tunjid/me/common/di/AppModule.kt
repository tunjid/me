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

import com.tunjid.me.common.data.AppDatabase
import com.tunjid.me.common.data.ByteSerializable
import com.tunjid.me.common.data.ByteSerializer
import com.tunjid.me.common.data.DelegatingByteSerializer
import com.tunjid.me.common.data.local.SessionCookieDao
import com.tunjid.me.common.data.local.SqlArchiveDao
import com.tunjid.me.common.data.local.SqlSessionCookieDao
import com.tunjid.me.common.data.local.databaseDispatcher
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.common.data.network.ApiUrl
import com.tunjid.me.common.data.network.NetworkMonitor
import com.tunjid.me.common.data.network.NetworkService
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
import com.tunjid.me.common.ui.archivedetail.ArchiveDetailRoute
import com.tunjid.me.common.ui.archiveedit.ArchiveEditRoute
import com.tunjid.me.common.ui.archivelist.ArchiveListRoute
import com.tunjid.me.common.ui.archivelist.State
import com.tunjid.me.common.ui.profile.ProfileRoute
import com.tunjid.me.common.ui.settings.SettingsRoute
import com.tunjid.me.common.ui.signin.SignInRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

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
 * Manual dependency injection module
 */
private class AppModule(
    override val appDatabase: AppDatabase,
    override val networkMonitor: NetworkMonitor,
    appScope: CoroutineScope,
    initialUiState: UiState
) : AppDependencies {

    override val archiveDao = SqlArchiveDao(
        database = appDatabase,
        dispatcher = databaseDispatcher(),
    )

    override val sessionCookieDao: SessionCookieDao = SqlSessionCookieDao(
        database = appDatabase,
        dispatcher = databaseDispatcher(),
    )

    val networkService: NetworkService = NetworkService(
        sessionCookieDao = sessionCookieDao
    )

    override val archiveRepository: ArchiveRepository = ReactiveArchiveRepository(
        networkService = networkService,
        appScope = appScope,
        networkMonitor = networkMonitor,
        dao = archiveDao
    )

    override val authRepository: AuthRepository =
        SessionCookieAuthRepository(
            networkService = networkService,
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
                    subclass(ArchiveListRoute::class)
                    subclass(ArchiveDetailRoute::class)
                    subclass(ArchiveEditRoute::class)
                    subclass(SignInRoute::class)
                    subclass(SettingsRoute::class)
                    subclass(ProfileRoute::class)
                }
                polymorphic(ByteSerializable::class) {
                    subclass(State::class)
                    subclass(com.tunjid.me.common.ui.archivedetail.State::class)
                    subclass(com.tunjid.me.common.ui.archiveedit.State::class)
                    subclass(com.tunjid.me.common.ui.settings.State::class)
                    subclass(com.tunjid.me.common.ui.signin.State::class)
                    subclass(com.tunjid.me.common.ui.profile.State::class)
                }
            }
        }
    )

    val routeMutatorFactory = AppMutatorFactory(
        appScope = appScope,
        appDependencies = this
    )

    init {
        appScope.launch {
            com.tunjid.me.common.data.network.modelEvents(
                url = "$ApiUrl/",
                dispatcher = databaseDispatcher()
            )
                .mapNotNull { event ->
                    val kind = when (event.collection) {
                        com.tunjid.me.core.model.ArchiveKind.Articles.type -> com.tunjid.me.core.model.ArchiveKind.Articles
                        com.tunjid.me.core.model.ArchiveKind.Talks.type -> com.tunjid.me.core.model.ArchiveKind.Talks
                        com.tunjid.me.core.model.ArchiveKind.Projects.type -> com.tunjid.me.core.model.ArchiveKind.Projects
                        else -> null
                    } ?: return@mapNotNull null

                    exponentialBackoff(
                        initialDelay = 1_000,
                        maxDelay = 20_000,
                        times = 5,
                        default = null
                    ) { networkService.fetchArchive(kind = kind, id = com.tunjid.me.core.model.ArchiveId(
                        event.id
                    )
                    ) }
                }
                .collect {
                    archiveDao.saveArchive(it)
                }
        }
    }

    override fun <T> routeDependencies(route: AppRoute<T>): T =
        routeMutatorFactory.routeMutator(route)
}