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

package com.tunjid.me.common.data.repository

import com.tunjid.me.common.data.model.ArchiveQuery
import com.tunjid.me.common.data.network.Api
import com.tunjid.me.common.data.network.NetworkMonitor
import com.tunjid.me.common.data.model.Archive
import com.tunjid.me.common.data.local.ArchiveDao
import com.tunjid.me.common.data.model.ArchiveKind
import com.tunjid.me.common.data.network.remoteFetcher
import com.tunjid.tiler.Tile
import kotlinx.coroutines.CoroutineScope
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
 * is to have a pub sub infrastructure such that when an entity changes on the server, the
 * app is notified and it pull it in.
 */
class ReactiveArchiveRepository(
    private val api: Api,
    appScope: CoroutineScope,
    networkMonitor: NetworkMonitor,
    private val dao: ArchiveDao
) : ArchiveRepository {

    private val remoteArchivesFetcher = remoteFetcher(
        scope = appScope,
        fetch = ::fetchArchives,
        save = dao::saveArchives,
        networkMonitor = networkMonitor
    )

    private val remoteArchiveFetcher = remoteFetcher(
        scope = appScope,
        fetch = { (kind, id): Pair<ArchiveKind, String> -> api.fetchArchive(kind = kind, id = id) },
        save = { dao.saveArchives(listOf(it)) },
        networkMonitor = networkMonitor
    )

    override fun monitorArchives(query: ArchiveQuery): Flow<List<Archive>> =
        dao.monitorArchives(query)
            .onStart { remoteArchivesFetcher(Tile.Request.On(query)) }
            .onCompletion { remoteArchivesFetcher(Tile.Request.Evict(query)) }

    override fun monitorArchive(kind: ArchiveKind, id: String): Flow<Archive> =
        dao.monitorArchive(kind = kind, id = id)
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
