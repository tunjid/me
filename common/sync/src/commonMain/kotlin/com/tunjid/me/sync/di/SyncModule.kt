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
import com.tunjid.me.common.sync.ChangeListItemQueries
import com.tunjid.me.core.sync.ChangeListKey
import com.tunjid.me.core.sync.Syncable
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.data.local.databaseDispatcher
import com.tunjid.me.data.network.NetworkMonitor
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.me.sync.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

typealias SyncableLocator = Map<ChangeListKey, Syncable>
typealias Sync = (ChangeListKey) -> Unit

class SyncModule(
    internal val appScope: CoroutineScope,
    internal val database: AppDatabase,
    internal val networkMonitor: NetworkMonitor,
)

@Component
abstract class InjectedSyncComponent(
    private val module: SyncModule,
    @Component val dataComponent: InjectedDataComponent,
) {
    @Provides
    fun sync(): Sync = synchronizer::sync

    @Provides
    fun networkMonitor(): NetworkMonitor = module.networkMonitor

    @Provides
    fun appScope(): CoroutineScope = module.appScope

    @Provides
    internal fun coroutineDispatcher(): CoroutineDispatcher = databaseDispatcher()

    @Provides
    fun changeListItemQueries(): ChangeListItemQueries = module.database.changeListItemQueries

    @Provides
    fun provideSyncableLocator(
        archiveRepository: ArchiveRepository
    ): SyncableLocator = mapOf(
        ChangeListKey.User to Syncable { _, _ -> },
        ChangeListKey.Archive.Articles to archiveRepository,
        ChangeListKey.Archive.Projects to archiveRepository,
        ChangeListKey.Archive.Talks to archiveRepository,
    )

    internal val InMemorySynchronizer.bind: Synchronizer
        @Provides get() = this

    internal val SqlChangeListDao.bind: ChangeListDao
        @Provides get() = this

    abstract val synchronizer: Synchronizer
}
