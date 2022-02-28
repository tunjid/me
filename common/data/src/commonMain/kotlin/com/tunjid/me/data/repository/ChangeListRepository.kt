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
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.stateFlowMutator
import com.tunjid.mutator.coroutines.toMutationStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal interface ChangeListRepository {
    fun sync(key: Keys.ChangeList)
}

internal interface ChangeListProcessor<in Key : Keys.ChangeList> {
    suspend fun process(key: Key, changeListItem: ChangeListItem): Boolean
}

private val allKeys = listOf(
    Keys.ChangeList.Users,
    Keys.ChangeList.Archive.Articles,
    Keys.ChangeList.Archive.Projects,
    Keys.ChangeList.Archive.Talks,
)

internal class SqlChangeListRepository(
    appScope: CoroutineScope,
    private val networkMonitor: NetworkMonitor,
    private val networkService: NetworkService,
    private val changeListDao: ChangeListDao,
    private val archiveChangeListProcessor: ChangeListProcessor<Keys.ChangeList.Archive>,
) : ChangeListRepository {

    init {
        appScope.launch {
            // Re-sync each time we're online
            networkMonitor.isConnected
                .distinctUntilChanged()
                .filter { it }
                .collect { allKeys.forEach(::sync) }
        }
    }
    private val mutator = stateFlowMutator<Keys.ChangeList, Unit>(
        scope = appScope,
        initialState = Unit,
        started = SharingStarted.Eagerly,
        actionTransform = { actions ->
            actions
                .onStart {
                    // Sync all keys on start
                    allKeys.forEach { emit(it) }
                }
                .toMutationStream(
                    keySelector = Keys.ChangeList::key,
                    transform = {
                        when (val key = type()) {
                            is Keys.ChangeList.Archive -> key.flow.chew(
                                networkService = networkService,
                                changeListDao = changeListDao,
                                archiveChangeListProcessor = archiveChangeListProcessor
                            )
                            // TODO: Process user changes
                            Keys.ChangeList.Users -> flowOf(NoOpMutation)
                        }
                    }
                )
        }
    )

    override fun sync(key: Keys.ChangeList) = mutator.accept(key)
}

/**
 * Chew through the change list and process it sequentially
 */
private fun Flow<Keys.ChangeList.Archive>.chew(
    networkService: NetworkService,
    changeListDao: ChangeListDao,
    archiveChangeListProcessor: ChangeListProcessor<Keys.ChangeList.Archive>
): Flow<Mutation<Unit>> =
    // New calls to sync should map latest, especially if it's in the middle of exp backoff
    mapLatest { key ->
        val id = changeListDao.latestId(key)
        val changeList = when (val response = networkService.changeList(key = key, id = id)) {
            is NetworkResponse.Success -> response.item
            is NetworkResponse.Error -> return@mapLatest NoOpMutation
        }
        changeList
            .takeWhile { item ->
                archiveChangeListProcessor.process(key = key, changeListItem = item)
            }
            .forEach { item ->
                changeListDao.markComplete(keys = key, item = item)
            }
        NoOpMutation
    }

private val NoOpMutation = Mutation<Unit> {}