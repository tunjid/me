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
import com.tunjid.me.core.model.ChangeListId
import com.tunjid.me.core.model.ChangeListItem
import com.tunjid.me.core.sync.ChangeListKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Dao for syncing change lists from the server
 */
internal interface ChangeListDao {
    /**
     * Last changeList processed
     */
    suspend fun latestItem(key: ChangeListKey): ChangeListItem?

    /**
     * Record that a change list has been processed
     */
    suspend fun markComplete(item: ChangeListItem)
}

internal class SqlChangeListDao(
    database: AppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : ChangeListDao {

    private val changeListItemQueries = database.changeListItemQueries

    override suspend fun latestItem(
        key: ChangeListKey
    ): ChangeListItem? = withContext(dispatcher) {
        changeListItemQueries.get(model = key.model)
            .executeAsOneOrNull()
            ?.let {
                ChangeListItem(
                    changeId = ChangeListId(it.changeId),
                    changeType = it.changeType,
                    modelId = it.modelId,
                    model = it.model,
                    id = it.id,
                )
            }
    }

    override suspend fun markComplete(
        item: ChangeListItem
    ) = withContext(dispatcher) {
        changeListItemQueries.upsert(
            id = item.id,
            changeId = item.changeId.value,
            changeType = item.changeType,
            modelId = item.modelId,
            model = item.model,
        )
    }
}