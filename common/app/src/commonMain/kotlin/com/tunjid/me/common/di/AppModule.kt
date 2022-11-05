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
import com.tunjid.me.core.utilities.UriConverter
import com.tunjid.me.data.network.NetworkMonitor
import com.tunjid.me.scaffold.permissions.PermissionsProvider
import kotlinx.coroutines.CoroutineScope
import okio.Path

//fun createAppDependencies(
//    appScope: CoroutineScope,
//    database: AppDatabase,
//    savedStatePath: Path,
//    permissionsProvider: PermissionsProvider,
//    networkMonitor: NetworkMonitor,
//    uriConverter: UriConverter,
//): AppDependencies = AppModule(
//    appDatabase = database,
//    savedStatePath = savedStatePath,
//    permissionsProvider = permissionsProvider,
//    networkMonitor = networkMonitor,
//    uriConverter = uriConverter,
//    appScope = appScope,
//)

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
) {


//    private val syncModule = SyncModule(
//        appScope = appScope,
//        networkMonitor = networkMonitor,
//        database = appDatabase,
//        locator = mapOf(
//            ChangeListKey.User to Syncable { _, _ -> },
//            ChangeListKey.Archive.Articles to dataComponent.archiveRepository,
//            ChangeListKey.Archive.Projects to dataComponent.archiveRepository,
//            ChangeListKey.Archive.Talks to dataComponent.archiveRepository,
//        )
//    )
//
//    private val syncComponent: SyncComponent = SyncComponent(
//        module = syncModule
//    )

//    init {
//        modelEvents(
//            url = "$ApiUrl/",
//            dispatcher = databaseDispatcher()
//        )
//            // This is an Android concern. Remove this when this is firebase powered.
//            .monitorWhenActive(scaffoldComponent.lifecycleStateStream)
//            .map { it.model.changeListKey() }
//            .onEach(syncComponent::sync)
//            .launchIn(appScope)
//    }
}