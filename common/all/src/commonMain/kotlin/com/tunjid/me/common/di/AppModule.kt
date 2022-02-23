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
import com.tunjid.me.archivedetail.State
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
import com.tunjid.me.scaffold.di.ScaffoldComponent
import com.tunjid.me.scaffold.di.ScaffoldModule
import com.tunjid.me.scaffold.globalui.UiState
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
        ArchiveDetailFeature,
        ArchiveListFeature,
    )

    private val scaffoldModule = ScaffoldModule(
        appScope = appScope,
        initialUiState = initialUiState,
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

    // TODO: Pass this as an argument
    override val byteSerializer: ByteSerializer = DelegatingByteSerializer(
        format = Cbor {
            serializersModule = SerializersModule {
                polymorphic(ByteSerializable::class) {
                    subclass(com.tunjid.me.feature.archivelist.State::class)
                    subclass(State::class)
                    subclass(com.tunjid.me.common.ui.archiveedit.State::class)
                    subclass(com.tunjid.me.common.ui.settings.State::class)
                    subclass(com.tunjid.me.common.ui.signin.State::class)
                    subclass(com.tunjid.me.common.ui.profile.State::class)
                }
            }
        }
    )

    override val routeServiceLocator: RouteServiceLocator = RouteMutatorFactory(
        appScope = appScope,
        features = features,
        scaffoldComponent = scaffoldComponent,
        dataComponent = dataComponent
    )

//    override fun <T : Mutator<*, *>> routeDependencies(route: AppRoute): T =
//        routeMutatorFactory.routeMutator(route)
}