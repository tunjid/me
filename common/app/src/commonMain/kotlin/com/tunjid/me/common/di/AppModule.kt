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

import com.tunjid.me.archivedetail.ArchiveDetailFeature
import com.tunjid.me.archiveedit.ArchiveEditFeature
import com.tunjid.me.common.data.AppDatabase
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.core.utilities.DelegatingByteSerializer
import com.tunjid.me.data.di.DataComponent
import com.tunjid.me.data.di.DataModule
import com.tunjid.me.data.network.NetworkMonitor
import com.tunjid.me.feature.RouteServiceLocator
import com.tunjid.me.feature.archivelist.ArchiveListFeature
import com.tunjid.me.profile.ProfileFeature
import com.tunjid.me.scaffold.di.ScaffoldComponent
import com.tunjid.me.scaffold.di.ScaffoldModule
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.settings.SettingsFeature
import com.tunjid.me.signin.SignInFeature
import kotlinx.coroutines.CoroutineScope
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
    initialUiState = initialUiState,
)

/**
 * Manual dependency injection module
 */
private class AppModule(
    appDatabase: AppDatabase,
    networkMonitor: NetworkMonitor,
    appScope: CoroutineScope,
    initialUiState: UiState,
) : AppDependencies {

    private val features = listOf(
        ArchiveEditFeature,
        ArchiveDetailFeature,
        ArchiveListFeature,
        ProfileFeature,
        SettingsFeature,
        SignInFeature,
    )

    override val byteSerializer: ByteSerializer = DelegatingByteSerializer(
        format = Cbor {
            serializersModule = SerializersModule {
                polymorphic(ByteSerializable::class) {
                    // TODO expose this on the feature directly
                    subclass(com.tunjid.me.feature.archivelist.State::class)
                    subclass(com.tunjid.me.archivedetail.State::class)
                    subclass(com.tunjid.me.archiveedit.State::class)
                    subclass(com.tunjid.me.settings.State::class)
                    subclass(com.tunjid.me.signin.State::class)
                    subclass(com.tunjid.me.profile.State::class)
                }
            }
        }
    )

    private val scaffoldModule = ScaffoldModule(
        appScope = appScope,
        initialUiState = initialUiState,
        byteSerializer = byteSerializer,
        startNav = (ArchiveKind.values().map {
            "archives/${it.type}"
        } + "settings")
            .map { listOf(it) },
        routeParsers = features
            .map { it.routeParsers }
            .flatten()
    )

    private val dataModule = DataModule(
        appScope = appScope,
        networkMonitor = networkMonitor,
        database = appDatabase,
    )
    override val scaffoldComponent: ScaffoldComponent = ScaffoldComponent(
        scaffoldModule
    )

    override val dataComponent: DataComponent = DataComponent(
        dataModule
    )

    override val routeServiceLocator: RouteServiceLocator = RouteMutatorFactory(
        appScope = appScope,
        features = features,
        scaffoldComponent = scaffoldComponent,
        dataComponent = dataComponent
    )
}