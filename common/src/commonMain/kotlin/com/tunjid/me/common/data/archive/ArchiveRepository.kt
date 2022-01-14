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
import com.tunjid.me.common.data.NetworkMonitor
import com.tunjid.me.common.data.remoteFetcher
import com.tunjid.tiler.Tile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

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
    networkMonitor: NetworkMonitor,
    dispatcher: CoroutineDispatcher,
) : ArchiveRepository {

    // TODO: This should be an interface that is passed in so it can be mocked in tests
    private val localArchiveRepository = ArchiveDataStore(
        database,
        dispatcher
    )

    private val remoteArchivesFetcher = remoteFetcher(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
        fetch = ::fetchArchives,
        save = localArchiveRepository::saveArchives,
        networkMonitor = networkMonitor
    )

    private val remoteArchiveFetcher = remoteFetcher(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
        fetch = { (kind, id): Pair<ArchiveKind, String> -> api.fetchArchive(kind = kind, id = id)},
        save = localArchiveRepository::saveArchive,
        networkMonitor = networkMonitor
    )

    override fun monitorArchives(query: ArchiveQuery): Flow<List<Archive>> =
        localArchiveRepository.monitorArchives(query)
            .onStart { remoteArchivesFetcher(Tile.Request.On(query)) }
            .onCompletion { remoteArchivesFetcher(Tile.Request.Evict(query)) }

    override fun monitorArchive(kind: ArchiveKind, id: String): Flow<Archive> =
        localArchiveRepository.monitorArchive(kind = kind, id = id)
            .onStart { remoteArchiveFetcher(Tile.Request.On(kind to id)) }
            .onCompletion { remoteArchiveFetcher(Tile.Request.Evict(kind to id)) }
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
