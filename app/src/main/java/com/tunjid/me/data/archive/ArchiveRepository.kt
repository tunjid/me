/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.me.data.archive

import com.tunjid.me.data.Api
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface ArchiveRepository {
    fun archives(query: ArchiveQuery): Flow<List<Archive>>
}

class RestArchiveRepository(
    private val api: Api
) : ArchiveRepository {
    override fun archives(query: ArchiveQuery): Flow<List<Archive>> = flow {
        val a = api.fetchArchives(
            kind = query.kind,
            options = listOfNotNull(
                "offset" to query.offset.toString(),
                "limit" to query.limit.toString(),
                query.filter?.let { "month" to it.month.toString() },
                query.filter?.let { "year" to it.year.toString() },
            ).toMap()
        )
        emit(
            api.fetchArchives(
                kind = query.kind,
                options = listOfNotNull(
                    "offset" to query.offset.toString(),
                    "limit" to query.limit.toString(),
                    query.filter?.let { "month" to it.month.toString() },
                    query.filter?.let { "year" to it.year.toString() },
                ).toMap()
            )
        )
    }
}