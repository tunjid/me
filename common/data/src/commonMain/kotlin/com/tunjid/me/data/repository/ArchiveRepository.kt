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

import com.tunjid.me.core.model.Archive
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveQuery
import com.tunjid.me.core.model.ArchiveUpsert
import com.tunjid.me.core.model.ChangeListItem
import com.tunjid.me.core.model.Result
import com.tunjid.me.core.model.isDelete
import com.tunjid.me.core.model.map
import com.tunjid.me.data.local.ArchiveDao
import com.tunjid.me.data.local.Keys
import com.tunjid.me.data.network.NetworkService
import com.tunjid.me.data.network.exponentialBackoff
import com.tunjid.me.data.network.models.item
import com.tunjid.me.data.network.models.toResult
import kotlinx.coroutines.flow.Flow

interface ArchiveRepository {
    suspend fun upsert(kind: ArchiveKind, upsert: ArchiveUpsert): Result<ArchiveId>
    fun monitorArchives(query: ArchiveQuery): Flow<List<Archive>>
    fun monitorArchive(kind: ArchiveKind, id: ArchiveId): Flow<Archive?>
}

/**
 * An implementation of [ArchiveRepository] that uses reactive pull. The long term goal
 * is to have a pub sub infrastructure such that when an entity changes on the server, the
 * app is notified, and pulls it in.
 */
internal class ReactiveArchiveRepository(
    private val networkService: NetworkService,
    private val dao: ArchiveDao
) : ArchiveRepository, ChangeListProcessor<Keys.ChangeList.Archive> {

    override suspend fun upsert(kind: ArchiveKind, upsert: ArchiveUpsert): Result<ArchiveId> =
        networkService.upsertArchive(kind, upsert)
            .toResult()
            .map { ArchiveId(it.id) }

    override fun monitorArchives(query: ArchiveQuery): Flow<List<Archive>> =
        dao.monitorArchives(query)

    override fun monitorArchive(kind: ArchiveKind, id: ArchiveId): Flow<Archive?> =
        dao.monitorArchive(kind = kind, id = id)

    override suspend fun process(key: Keys.ChangeList.Archive, changeListItem: ChangeListItem): Boolean {
        if (changeListItem.isDelete()) {
            dao.deleteArchives(listOf(ArchiveId(changeListItem.modelId)))
            return true
        }

        val archive = exponentialBackoff(
            initialDelay = 1_000,
            maxDelay = 20_000,
            default = null,
        ) {
            networkService.fetchArchive(
                kind = key.kind,
                id = ArchiveId(changeListItem.modelId)
            ).item()
        } ?: return false

        dao.saveArchives(listOf(archive))
        return true
    }

//    private suspend fun fetchArchives(query: ArchiveQuery): List<Archive> =
//        networkService.fetchArchives(
//            kind = query.kind,
//            options = listOfNotNull(
//                "offset" to query.offset.toString(),
//                "limit" to query.limit.toString(),
//                query.temporalFilter?.let { "month" to it.month.toString() },
//                query.temporalFilter?.let { "year" to it.year.toString() },
//            ).toMap(),
//            tags = query.contentFilter.tags,
//            categories = query.contentFilter.categories,
//        ).item() ?: listOf()
}
