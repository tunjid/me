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

package com.tunjid.me.data.local

import com.tunjid.me.common.data.AppDatabase
import com.tunjid.me.core.model.ChangeListId
import com.tunjid.me.core.model.ChangeListItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext


/**
 * Dao for syncing change lists from the server
 */
internal interface ChangeListDao {
    suspend fun latestId(keys: Keys.ChangeList): ChangeListId?
    suspend fun markComplete(keys: Keys.ChangeList, item: ChangeListItem)
}

internal class SqlChangeListDao(
    database: AppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : ChangeListDao {

    private val keyValueQueries = database.keyValueQueries

    override suspend fun latestId(keys: Keys.ChangeList): ChangeListId? = withContext(dispatcher) {
        keyValueQueries.find(keys.key).executeAsOneOrNull()
            ?.data_
            ?.let(::ChangeListId)
    }

    override suspend fun markComplete(keys: Keys.ChangeList, item: ChangeListItem) = withContext(dispatcher) {
        keyValueQueries.upsert(keys.key, item.id.value)
    }
}