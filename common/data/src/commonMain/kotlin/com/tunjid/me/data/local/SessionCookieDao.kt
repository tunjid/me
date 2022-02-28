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

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import com.tunjid.me.common.data.AppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Dao for auth sessions
 */
internal interface SessionCookieDao {
    val sessionCookieStream: Flow<String?>
    suspend fun saveSessionCookie(sessionCookie: String?)
}

internal class SqlSessionCookieDao(
    database: AppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : SessionCookieDao {
    private val keyValueQueries = database.keyValueQueries

    override val sessionCookieStream: Flow<String?> =
        keyValueQueries
            .find(id = Keys.SessionCookieId.key)
            .asFlow()
            .mapToOneOrNull(context = dispatcher)
            .map { it?.data_ }

    override suspend fun saveSessionCookie(sessionCookie: String?) {
        keyValueQueries.suspendingTransaction(context = dispatcher) {
            keyValueQueries.upsert(
                id = Keys.SessionCookieId.key,
                data = sessionCookie
            )
        }
    }
}