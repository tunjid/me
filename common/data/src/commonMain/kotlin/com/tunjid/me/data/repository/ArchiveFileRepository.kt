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

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.tunjid.me.common.data.*
import com.tunjid.me.core.model.*
import com.tunjid.me.core.sync.ChangeListKey
import com.tunjid.me.core.sync.SyncRequest
import com.tunjid.me.core.sync.Syncable
import com.tunjid.me.core.sync.changeListKey
import com.tunjid.me.core.utilities.Uri
import com.tunjid.me.core.utilities.UriConverter
import com.tunjid.me.core.utilities.fileDesc
import com.tunjid.me.data.local.models.toExternalModel
import com.tunjid.me.data.local.suspendingTransaction
import com.tunjid.me.data.network.NetworkService
import com.tunjid.me.data.network.models.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import me.tatarka.inject.annotations.Inject

interface ArchiveFileRepository : Syncable {
    fun photos(
        query: ArchiveFileQuery,
    ): Flow<List<ArchiveFile>>

    suspend fun uploadArchiveFile(
        kind: ArchiveKind,
        id: ArchiveId,
        uri: Uri,
    ): Result<Unit>
}

/**
 * An implementation of [ArchiveRepository] that uses reactive pull. The long term goal
 * is to have a pub sub infrastructure such that when an entity changes on the server, the
 * app is notified, and pulls it in.
 */
@Inject
internal class OfflineFirstArchiveFileRepository(
    private val networkService: NetworkService,
    private val uriConverter: UriConverter,
    private val archiveFileEntityQueries: ArchiveFileEntityQueries,
    private val archiveEntityQueries: ArchiveEntityQueries,
    private val archiveAuthorQueries: UserEntityQueries,
    private val dispatcher: CoroutineDispatcher,
) : ArchiveFileRepository {

    override fun photos(
        query: ArchiveFileQuery,
    ): Flow<List<ArchiveFile>> = archiveFileEntityQueries.photos(
        archiveId = query.archiveId.value,
        desc = query.desc,
        limit = query.limit.toLong(),
        offset = query.offset.toLong(),
    )
        .asFlow()
        .mapToList(context = dispatcher)
        .map { archiveFileEntities ->
            archiveFileEntities.map {
                ArchiveFile(
                    id = ArchiveFileId(it.id),
                    archiveId = ArchiveId(it.archive_id),
                    url = it.url,
                    mimeType = it.mimetype,
                    created = Instant.fromEpochMilliseconds(it.created)
                )
            }
        }
        .distinctUntilChanged()

    override suspend fun uploadArchiveFile(
        kind: ArchiveKind,
        id: ArchiveId,
        uri: Uri,
    ): Result<Unit> = networkService.uploadArchiveHeaderPhoto(
        kind = kind,
        id = id,
        fileDesc = uriConverter.fileDesc(uri)
    )
        .toResult()
        .map { }

    override suspend fun syncWith(
        request: SyncRequest,
        onVersionUpdated: suspend (ChangeListItem) -> Unit,
    ) {
        val changeList = when (val response = networkService.changeList(request)) {
            is NetworkResponse.Success -> response.item
            is NetworkResponse.Error -> throw Exception(response.message)
        }
        // Chew through the change list and process it sequentially
        changeList.chunked(10)
            .takeWhile { items ->
                val (deleted, updated) = items.partition(ChangeListItem::isDelete)

                archiveFileEntityQueries.suspendingTransaction(context = dispatcher) {
                    deleted
                        .map(ChangeListItem::modelId)
                        .map(::ArchiveFileId)
                        .map(ArchiveFileId::value)
                        .forEach(archiveFileEntityQueries::delete)
                }

                val archiveFiles = networkService.fetchArchiveFiles(
                    kind = (request.model.changeListKey() as ChangeListKey.ArchiveFile).kind,
                    ids = updated.map(ChangeListItem::modelId).map(::ArchiveFileId)
                ).item()

                if (archiveFiles != null) {
                    archiveFileEntityQueries.suspendingTransaction(context = dispatcher) {
                        archiveFiles.forEach(::saveNetworkArchiveFile)
                    }
                    true
                } else false
            }
            .forEach { items ->
                onVersionUpdated(items.last())
            }
    }

    private fun saveNetworkArchiveFile(networkArchiveFile: NetworkArchiveFile) {
        archiveAuthorQueries.insertOrIgnore(networkArchiveFile.uploaderShell())
        archiveEntityQueries.insertOrIgnore(networkArchiveFile.archiveShell())
        archiveFileEntityQueries.upsert(networkArchiveFile.toEntity())
    }

}
