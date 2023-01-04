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

package com.tunjid.me.sync

import com.tunjid.me.common.sync.AppDatabase
import com.tunjid.me.core.model.ChangeListItem
import com.tunjid.me.core.sync.ChangeListKey
import com.tunjid.me.core.sync.SyncRequest
import com.tunjid.me.core.sync.Syncable
import com.tunjid.me.data.local.databaseDispatcher
import com.tunjid.me.data.network.NetworkMonitor
import com.tunjid.me.data.network.exponentialBackoff
import com.tunjid.me.sync.di.SyncableLocator
import com.tunjid.mutator.coroutines.splitByType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject


interface Synchronizer {

    val onVersionUpdated: suspend (ChangeListItem) -> Unit

    fun sync(key: ChangeListKey)

    /**
     * Syntactic sugar to call [Syncable.syncWith] while omitting the synchronizer argument
     */
    suspend fun Syncable.sync(request: SyncRequest) = this@sync.syncWith(
        request = request,
        onVersionUpdated = onVersionUpdated
    )
}

expect fun synchronizer(
    appScope: CoroutineScope,
    database: AppDatabase,
    locator: SyncableLocator,
    networkMonitor: NetworkMonitor,
): Synchronizer

internal fun commonSynchronizer(
    appScope: CoroutineScope,
    database: AppDatabase,
    locator: SyncableLocator,
    networkMonitor: NetworkMonitor,
): Synchronizer = InMemorySynchronizer(
    appScope = appScope,
    networkMonitor = networkMonitor,
    locator = locator,
    changeListDao = SqlChangeListDao(
        changeListItemQueries = database.changeListItemQueries,
        dispatcher = databaseDispatcher(),
    )
)

private val allChangeListKey = listOf(
    ChangeListKey.ArchiveFile.Articles,
    ChangeListKey.ArchiveFile.Projects,
    ChangeListKey.ArchiveFile.Talks,
    ChangeListKey.Archive.Articles,
    ChangeListKey.Archive.Projects,
    ChangeListKey.Archive.Talks,
    ChangeListKey.User,
)

@Inject
internal class InMemorySynchronizer(
    private val appScope: CoroutineScope,
    networkMonitor: NetworkMonitor,
    private val locator: SyncableLocator,
    private val changeListDao: ChangeListDao,
) : Synchronizer {

    private val input = MutableSharedFlow<ChangeListKey>()
    override val onVersionUpdated: suspend (ChangeListItem) -> Unit = changeListDao::markComplete

    init {
        // Process sync requests for each key in parallel.
        input.splitByType(
            typeSelector = { it },
            keySelector = ChangeListKey::model,
            transform = {
                val key = type()
                key.flow.mapLatest {
                    val latestItem = changeListDao.latestItem(key)
                    exponentialBackoff(
                        initialDelay = 1_000,
                        maxDelay = 20_000,
                        default = Unit,
                    ) {
                        locator[key]?.sync(
                            SyncRequest(
                                model = key.model,
                                after = latestItem
                            )
                        ) ?: Unit
                    }
                }
            }
        )
            .launchIn(appScope)

        // Re-sync each time we're online
        networkMonitor.isConnected
            .distinctUntilChanged()
            .filter { it }
            .flatMapLatest { allChangeListKey.asFlow() }
            .onEach(::sync)
            .launchIn(appScope)
    }

    override fun sync(key: ChangeListKey) {
        appScope.launch {
            input.emit(key)
        }
    }
}
