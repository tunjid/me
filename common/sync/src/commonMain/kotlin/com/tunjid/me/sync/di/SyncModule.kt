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

package com.tunjid.me.sync.di

import com.tunjid.me.common.sync.AppDatabase
import com.tunjid.me.core.sync.ChangeListKey
import com.tunjid.me.core.sync.Syncable
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.data.network.NetworkMonitor
import com.tunjid.me.sync.Synchronizer
import com.tunjid.me.sync.synchronizer
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Component

typealias SyncableLocator = Map<ChangeListKey, Syncable>

class SyncModule(
    appScope: CoroutineScope,
    database: AppDatabase,
    locator: SyncableLocator,
    networkMonitor: NetworkMonitor,
) {
    val synchronizer: Synchronizer = synchronizer(
        appScope,
        database,
        locator,
        networkMonitor
    )
}

class SyncComponent(
    private val module: SyncModule
) {
    fun sync(key: ChangeListKey) = module.synchronizer.sync(key)
}


@Component
class InjectedSyncComponent(
    private val module: SyncModule,
    @Component dataComponent: InjectedDataComponent,
) {
    fun sync(key: ChangeListKey) = module.synchronizer.sync(key)
}
