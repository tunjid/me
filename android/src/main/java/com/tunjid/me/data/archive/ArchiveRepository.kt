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

package com.tunjid.me.data.archive

import com.tunjid.me.data.Api
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface ArchiveRepository {
    fun monitorArchives(query: ArchiveQuery): Flow<List<Archive>>
    fun monitorArchive(kind: ArchiveKind, id: String): Flow<Archive>
}

class RestArchiveRepository(
    private val api: Api
) : ArchiveRepository {

    override fun monitorArchives(query: ArchiveQuery): Flow<List<Archive>> = flow {
        emit(
            api.fetchArchives(
                kind = query.kind,
                options = listOfNotNull(
                    "offset" to query.offset.toString(),
                    "limit" to query.limit.toString(),
                    query.temporalFilter?.let { "month" to it.month.toString() },
                    query.temporalFilter?.let { "year" to it.year.toString() },
                ).toMap(),
                tags = query.contentFilter.tags.map(Descriptor.Tag::value),
                categories = query.contentFilter.categories.map(Descriptor.Category::value),
            )
        )
    }

    override fun monitorArchive(kind: ArchiveKind, id: String): Flow<Archive> = flow {
        emit(api.fetchArchive(kind = kind, id = id))
    }
}
