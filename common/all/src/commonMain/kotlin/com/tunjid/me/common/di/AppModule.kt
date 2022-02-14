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
import com.tunjid.me.common.data.network.NetworkMonitor
import com.tunjid.me.common.ui.archivedetail.ArchiveDetailRoute
import com.tunjid.me.common.ui.archiveedit.ArchiveEditRoute
import com.tunjid.me.common.ui.archivelist.ArchiveListRoute
import com.tunjid.me.common.ui.archivelist.State
import com.tunjid.me.common.ui.profile.ProfileRoute
import com.tunjid.me.common.ui.settings.SettingsRoute
import com.tunjid.me.common.ui.signin.SignInRoute
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.core.utilities.DelegatingByteSerializer
import com.tunjid.me.data.DataModule
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.globalui.UiState
import com.tunjid.me.globalui.globalUiMutator
import com.tunjid.me.nav.AppRoute
import com.tunjid.me.nav.ByteSerializableRoute
import com.tunjid.me.nav.navMutator
import com.tunjid.mutator.Mutator
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

private val startNav = MultiStackNav(
    name = "NavName",
    currentIndex = 0,
    stacks = ArchiveKind.values().map { kind ->
        StackNav(
            name = kind.name,
            routes = listOf(ArchiveListRoute(kind = kind))
        )
    } + StackNav(
        name = "Settings",
        routes = listOf(SettingsRoute)
    )
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
    initialUiState = initialUiState,
)

/**
 * Manual dependency injection module
 */
private class AppModule(
    appDatabase: AppDatabase,
    override val networkMonitor: NetworkMonitor,
    appScope: CoroutineScope,
    initialUiState: UiState,
) : AppDependencies {

    private val dataModule = DataModule(
        appScope = appScope,
        networkMonitor = networkMonitor,
        database = appDatabase,
    )

    override val archiveRepository: ArchiveRepository = dataModule.archiveRepository

    override val authRepository: AuthRepository = dataModule.authRepository

    override val appMutator: AppMutator = appMutator(
        scope = appScope,
        navMutator = navMutator(scope = appScope, startNav = startNav),
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

    override fun <T : Mutator<*, *>> routeDependencies(route: AppRoute<T>): T =
        routeMutatorFactory.routeMutator(route)
}