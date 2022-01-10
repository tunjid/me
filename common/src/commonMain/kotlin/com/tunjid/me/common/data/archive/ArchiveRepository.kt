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

package com.tunjid.me.common.data.archive

import com.tunjid.me.common.data.Api
import com.tunjid.me.common.data.AppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach

interface ArchiveRepository {
    fun monitorArchives(query: ArchiveQuery): Flow<List<Archive>>
    fun monitorArchive(kind: ArchiveKind, id: String): Flow<Archive>
}

/**
 * An implementation of [ArchiveRepository] that uses reactive pull. The long term goal
 * is to have a oub sub infrastructure such that when an entity changes on the server, the
 * app is notified and it pull it in.
 */
class ReactiveArchiveRepository(
    private val api: Api,
    database: AppDatabase,
    dispatcher: CoroutineDispatcher,
) : ArchiveRepository {

    // TODO: This should be an interface that is passed in so it can be mocked in tests
    private val localArchiveRepository = LocalArchiveRepository(
        database,
        dispatcher
    )

    override fun monitorArchives(query: ArchiveQuery): Flow<List<Archive>> =
        localArchiveRepository.monitorArchives(query)
            .onEach { archives ->
                // Oof nothing in the DB, fetch it as a side effect!
                // TODO: This never invalidates the cache. Fix this
                if (archives.isEmpty()) try {
                    localArchiveRepository.saveArchives(
                        archives = fetchArchives(query)
                    )
                } catch (e: Throwable) {
                    // TODO: exponential back off
                    e.printStackTrace()
                }
            }

    override fun monitorArchive(kind: ArchiveKind, id: String): Flow<Archive> =
        localArchiveRepository.monitorArchive(kind = kind, id = id)
            .onEach { archive ->
                // Oof nothing in the DB, fetch it as a side effect!
                if (archive == null) try {
                    localArchiveRepository.saveArchive(
                        api.fetchArchive(kind = kind, id = id)
                    )
                } catch (e: Throwable) {
                    // TODO: exponential back off
                    e.printStackTrace()
                }
            }
            .filterNotNull()

    private suspend fun fetchArchives(query: ArchiveQuery): List<Archive> =
        api.fetchArchives(
            kind = query.kind,
            options = listOfNotNull(
                "offset" to query.offset.toString(),
                "limit" to query.limit.toString(),
                query.temporalFilter?.let { "month" to it.month.toString() },
                query.temporalFilter?.let { "year" to it.year.toString() },
            ).toMap(),
            tags = query.contentFilter.tags,
            categories = query.contentFilter.categories,
        )
}
