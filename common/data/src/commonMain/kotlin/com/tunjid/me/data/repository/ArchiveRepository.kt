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
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.tunjid.me.common.data.*
import com.tunjid.me.core.model.*
import com.tunjid.me.core.sync.ChangeListKey
import com.tunjid.me.core.sync.SyncRequest
import com.tunjid.me.core.sync.Syncable
import com.tunjid.me.core.sync.changeListKey
import com.tunjid.me.core.utilities.LocalUri
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

interface ArchiveRepository : Syncable {
    suspend fun upsert(kind: ArchiveKind, upsert: ArchiveUpsert): Result<ArchiveId>
    suspend fun uploadArchiveHeaderPhoto(
        kind: ArchiveKind,
        id: ArchiveId,
        uri: LocalUri,
    ): Result<Unit>

    fun count(query: ArchiveQuery): Flow<Long>

    fun descriptorsMatching(
        descriptor: Descriptor,
        kind: ArchiveKind,
    ): Flow<List<Descriptor>>

    fun archivesStream(
        query: ArchiveQuery,
    ): Flow<List<Archive>>

    fun archiveStream(
        id: ArchiveId,
    ): Flow<Archive?>

    fun offsetForId(
        id: ArchiveId,
        query: ArchiveQuery,
    ): Flow<Long>
}

/**
 * An implementation of [ArchiveRepository] that uses reactive pull. The long term goal
 * is to have a pub sub infrastructure such that when an entity changes on the server, the
 * app is notified, and pulls it in.
 */
@Inject
internal class OfflineFirstArchiveRepository(
    private val networkService: NetworkService,
    private val uriConverter: UriConverter,
    private val archiveEntityQueries: ArchiveEntityQueries,
    private val archiveTagQueries: ArchiveTagEntityQueries,
    private val archiveCategoryQueries: ArchiveCategoryEntityQueries,
    private val archiveAuthorQueries: UserEntityQueries,
    private val dispatcher: CoroutineDispatcher,
) : ArchiveRepository {

    override suspend fun upsert(kind: ArchiveKind, upsert: ArchiveUpsert): Result<ArchiveId> =
        networkService.upsertArchive(kind, upsert)
            .toResult()
            .map { ArchiveId(it.id) }

    override suspend fun uploadArchiveHeaderPhoto(
        kind: ArchiveKind,
        id: ArchiveId,
        uri: LocalUri,
    ): Result<Unit> = networkService.uploadArchiveHeaderPhoto(
        kind = kind,
        id = id,
        fileDesc = uriConverter.fileDesc(uri)
    )
        .toResult()
        .map { }

    override fun archivesStream(
        query: ArchiveQuery,
    ): Flow<List<Archive>> = archiveEntityQueries.find(
        kind = query.kind.type,
        desc = query.desc,
        limit = query.limit.toLong(),
        offset = query.offset.toLong(),
        useFilters = query.hasContentFilter,
        tagsOrCategories = query.contentFilter.tags.map(Descriptor.Tag::value)
            .plus(query.contentFilter.categories.map(Descriptor.Category::value))
            .distinct()
    )
        .asFlow()
        .mapToList(context = dispatcher)
        .flatMapLatest { archiveEntities -> archiveEntitiesToArchives(archiveEntities) }
        .distinctUntilChanged()

    override fun count(
        query: ArchiveQuery,
    ): Flow<Long> = archiveEntityQueries.count(
        kind = query.kind.type,
        useFilters = query.hasContentFilter,
        tagsOrCategories = query.contentFilter.tags.map(Descriptor.Tag::value)
            .plus(query.contentFilter.categories.map(Descriptor.Category::value))
            .distinct()
    )
        .asFlow()
        .mapToOneOrNull(context = dispatcher)
        .filterNotNull()

    override fun descriptorsMatching(
        descriptor: Descriptor,
        kind: ArchiveKind,
    ): Flow<List<Descriptor>> =
        combine(
            flow = archiveCategoryQueries.categoriesContaining(
                subString = "%${descriptor.value}%",
                kind = kind.type,
            )
                .asFlow()
                .mapToList(context = dispatcher)
                .map { categoriesContainingList ->
                    categoriesContainingList.map {
                        Descriptor.Category(
                            it.category
                        )
                    }
                },
            flow2 = archiveTagQueries.tagsContaining(
                subString = "%${descriptor.value}%",
                kind = kind.type,
            )
                .asFlow()
                .mapToList(context = dispatcher)
                .map { tagsContainingList -> tagsContainingList.map { Descriptor.Tag(it.tag) } },
            transform = { categories, tags ->
                when (descriptor) {
                    is Descriptor.Category -> categories + tags
                    is Descriptor.Tag -> tags + categories
                }
            }
        )

    override fun archiveStream(
        id: ArchiveId,
    ): Flow<Archive?> = archiveEntityQueries.get(
        id = id.value,
    )
        .asFlow()
        .mapToOneOrNull(context = dispatcher)
        .flatMapLatest { it?.let(::archiveEntityToArchive) ?: flowOf(null) }
        .distinctUntilChanged()

    override fun offsetForId(
        id: ArchiveId,
        query: ArchiveQuery,
    ): Flow<Long> =
        archiveEntityQueries.offsetForId(
            id = id.value,
            kind = query.kind.type,
            useFilters = query.hasContentFilter,
            desc = query.desc,
            tagsOrCategories = query.contentFilter.tags.map(Descriptor.Tag::value)
                .plus(query.contentFilter.categories.map(Descriptor.Category::value))
                .distinct()
        )
            .asFlow()
            .mapToOne(context = dispatcher)

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

                archiveAuthorQueries.suspendingTransaction(context = dispatcher) {
                    deleted
                        .map(ChangeListItem::modelId)
                        .map(::ArchiveId)
                        .map(ArchiveId::value)
                        .forEach(archiveEntityQueries::delete)
                }

                val archives = networkService.fetchArchives(
                    kind = (request.model.changeListKey() as ChangeListKey.Archive).kind,
                    ids = updated.map(ChangeListItem::modelId).map(::ArchiveId)
                ).item()

                if (archives != null) {
                    archiveAuthorQueries.suspendingTransaction(context = dispatcher) {
                        archives.forEach(::saveNetworkArchive)
                    }
                    true
                } else false
            }
            .forEach { items ->
                onVersionUpdated(items.last())
            }
    }

    private fun archiveEntitiesToArchives(list: List<ArchiveEntity>): Flow<List<Archive>> =
        if (list.isEmpty()) flowOf(listOf()) else combine(
            flows = list.map(::archiveEntityToArchive),
            transform = Array<Archive>::toList
        )

    private fun archiveEntityToArchive(archiveEntity: ArchiveEntity): Flow<Archive> =
        combine(
            flow = this.archiveTagQueries.find(archive_id = archiveEntity.id)
                .asFlow()
                .mapToList(context = this.dispatcher),
            flow2 = this.archiveCategoryQueries.find(archive_id = archiveEntity.id)
                .asFlow()
                .mapToList(context = this.dispatcher),
            flow3 = this.archiveAuthorQueries.find(id = archiveEntity.author)
                .asFlow()
                .mapToOne(context = this.dispatcher),
        ) { tags, categories, author ->
            archiveEntity.toExternalModel(
                author = author,
                tags = tags,
                categories = categories
            )
        }

    private fun saveNetworkArchive(networkArchive: NetworkArchive) {
        archiveAuthorQueries.insertOrIgnore(networkArchive.authorShell())
        archiveEntityQueries.upsert(networkArchive.toEntity())

        archiveTagQueries.delete(networkArchive.id.value)
        networkArchive.tags.forEach { tag ->
            archiveTagQueries.upsert(
                archive_id = networkArchive.id.value,
                tag = tag.value,
            )
        }

        archiveCategoryQueries.delete(networkArchive.id.value)
        networkArchive.categories.forEach { category ->
            archiveCategoryQueries.upsert(
                archive_id = networkArchive.id.value,
                category = category.value,
            )
        }
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
