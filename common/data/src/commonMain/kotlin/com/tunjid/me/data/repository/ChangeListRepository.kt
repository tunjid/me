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

package com.tunjid.me.data.repository

import com.tunjid.me.core.model.ChangeListItem
import com.tunjid.me.data.local.ChangeListDao
import com.tunjid.me.data.local.Keys
import com.tunjid.me.data.network.NetworkMonitor
import com.tunjid.me.data.network.NetworkService
import com.tunjid.me.data.network.models.NetworkResponse
import com.tunjid.mutator.coroutines.splitByType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Syncs [ChangeListItem] from the server and incrementally applies their updates
 */
internal interface ChangeListRepository {
    fun sync(key: Keys.ChangeList)
}

/**
 * Processes a [ChangeListItem]
 */
internal interface ChangeListProcessor<in Key : Keys.ChangeList> {
    suspend fun process(key: Key, changeList: List<ChangeListItem>): Boolean
}

private val allKeys = listOf(
    Keys.ChangeList.User,
    Keys.ChangeList.Archive.Articles,
    Keys.ChangeList.Archive.Projects,
    Keys.ChangeList.Archive.Talks,
)

internal class SqlChangeListRepository(
    private val appScope: CoroutineScope,
    networkMonitor: NetworkMonitor,
    private val networkService: NetworkService,
    private val changeListDao: ChangeListDao,
    private val archiveChangeListProcessor: ChangeListProcessor<Keys.ChangeList.Archive>,
) : ChangeListRepository {

    private val input = MutableSharedFlow<Keys.ChangeList>()

    init {
        // Process sync requests for each key in parallel.
        input.splitByType(
            typeSelector = { it },
            keySelector = Keys.ChangeList::key,
            transform = {
                when (val key = type()) {
                    is Keys.ChangeList.Archive -> key.flow.chew(
                        networkService = networkService,
                        changeListDao = changeListDao,
                        changeListProcessor = archiveChangeListProcessor
                    )
                    // TODO: Process user changes
                    Keys.ChangeList.User -> emptyFlow()
                }
            }
        )
            .launchIn(appScope)

        // Re-sync each time we're online
        networkMonitor.isConnected
            .distinctUntilChanged()
            .filter { it }
            .flatMapLatest { allKeys.asFlow() }
            .onEach { sync(it) }
            .launchIn(appScope)
    }

    override fun sync(key: Keys.ChangeList) {
        appScope.launch {
            input.emit(key)
        }
    }
}

/**
 * Chew through the change list and process it sequentially
 */
private fun Flow<Keys.ChangeList.Archive>.chew(
    networkService: NetworkService,
    changeListDao: ChangeListDao,
    changeListProcessor: ChangeListProcessor<Keys.ChangeList.Archive>
): Flow<Unit> =
    // New calls to sync should map latest, especially if it's in the middle of exp backoff bc of network loss.
    mapLatest { key ->
        val id = changeListDao.latestId(key)
        val changeList = when (val response = networkService.changeList(key = key, id = id)) {
            is NetworkResponse.Success -> response.item
            is NetworkResponse.Error -> return@mapLatest
        }
        changeList.chunked(10)
            .takeWhile { items ->
                changeListProcessor.process(key = key, changeList = items)
            }
            .forEach { items ->
                changeListDao.markComplete(keys = key, item = items.last())
            }
    }
