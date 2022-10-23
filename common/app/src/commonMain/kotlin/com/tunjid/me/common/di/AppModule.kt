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

import com.tunjid.me.AppDatabase
import com.tunjid.me.archivedetail.ArchiveDetailFeature
import com.tunjid.me.archiveedit.ArchiveEditFeature
import com.tunjid.me.core.sync.ChangeListKey
import com.tunjid.me.core.sync.Syncable
import com.tunjid.me.core.sync.changeListKey
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.core.utilities.DelegatingByteSerializer
import com.tunjid.me.core.utilities.UriConverter
import com.tunjid.me.data.di.DataComponent
import com.tunjid.me.data.di.DataModule
import com.tunjid.me.data.local.databaseDispatcher
import com.tunjid.me.data.network.ApiUrl
import com.tunjid.me.data.network.NetworkMonitor
import com.tunjid.me.data.network.modelEvents
import com.tunjid.me.feature.Feature
import com.tunjid.me.feature.RouteServiceLocator
import com.tunjid.me.feature.archivelist.ArchiveListFeature
import com.tunjid.me.profile.ProfileFeature
import com.tunjid.me.scaffold.di.ScaffoldComponent
import com.tunjid.me.scaffold.di.ScaffoldModule
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.lifecycle.monitorWhenActive
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.permissions.PermissionsProvider
import com.tunjid.me.settings.SettingsFeature
import com.tunjid.me.signin.SignInFeature
import com.tunjid.me.sync.di.SyncComponent
import com.tunjid.me.sync.di.SyncModule
import com.tunjid.mutator.ActionStateProducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.protobuf.ProtoBuf
import okio.Path

fun createAppDependencies(
    appScope: CoroutineScope,
    initialUiState: UiState = UiState(),
    database: AppDatabase,
    savedStatePath: Path,
    permissionsProvider: PermissionsProvider,
    networkMonitor: NetworkMonitor,
    uriConverter: UriConverter,
): AppDependencies = AppModule(
    appDatabase = database,
    savedStatePath = savedStatePath,
    permissionsProvider = permissionsProvider,
    networkMonitor = networkMonitor,
    uriConverter = uriConverter,
    appScope = appScope,
    initialUiState = initialUiState,
)

/**
 * Manual dependency injection module
 */
private class AppModule(
    appDatabase: AppDatabase,
    savedStatePath: Path,
    permissionsProvider: PermissionsProvider,
    networkMonitor: NetworkMonitor,
    uriConverter: UriConverter,
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
        format = ProtoBuf {
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
        savedStatePath = savedStatePath,
        initialUiState = initialUiState,
        permissionsProvider = permissionsProvider,
        byteSerializer = byteSerializer,
        uriConverter = uriConverter,
        routeMatchers = features
            .map(Feature<out AppRoute, out ActionStateProducer<out Any, out StateFlow<*>>>::routeMatchers)
            .flatten()
    )

    private val dataModule = DataModule(
        database = appDatabase,
        uriConverter = uriConverter,
    )

    override val scaffoldComponent: ScaffoldComponent = ScaffoldComponent(
        scaffoldModule
    )

    override val dataComponent: DataComponent = DataComponent(
        dataModule
    )

    private val syncModule = SyncModule(
        appScope = appScope,
        networkMonitor = networkMonitor,
        database = appDatabase,
        locator = mapOf(
            ChangeListKey.User to Syncable { _, _ -> },
            ChangeListKey.Archive.Articles to dataComponent.archiveRepository,
            ChangeListKey.Archive.Projects to dataComponent.archiveRepository,
            ChangeListKey.Archive.Talks to dataComponent.archiveRepository,
        )
    )

    private val syncComponent: SyncComponent = SyncComponent(
        module = syncModule
    )

    override val routeServiceLocator: RouteServiceLocator = RouteMutatorFactory(
        appScope = appScope,
        features = features,
        byteSerializer = byteSerializer,
        scaffoldComponent = scaffoldComponent,
        dataComponent = dataComponent
    )

    init {
        modelEvents(
            url = "$ApiUrl/",
            dispatcher = databaseDispatcher()
        )
            // This is an Android concern. Remove this when this is firebase powered.
            .monitorWhenActive(scaffoldComponent.lifecycleStateStream)
            .map { it.model.changeListKey() }
            .onEach(syncComponent::sync)
            .launchIn(appScope)
    }
}