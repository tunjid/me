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
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import com.tunjid.me.common.data.AppDatabase
import com.tunjid.me.common.data.ArchiveEntity
import com.tunjid.me.core.model.Archive
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.Descriptor.Category
import com.tunjid.me.core.model.Descriptor.Tag
import com.tunjid.me.core.model.hasContentFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant

internal interface ArchiveDao {
    fun monitorArchives(query: com.tunjid.me.core.model.ArchiveQuery): Flow<List<Archive>>
    fun monitorArchive(
        kind: ArchiveKind,
        id: ArchiveId
    ): Flow<Archive?>

    suspend fun saveArchives(archives: List<Archive>)
    suspend fun deleteArchives(ids: List<ArchiveId>)
}

internal class SqlArchiveDao(
    database: AppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : ArchiveDao {

    private val archiveQueries = database.archiveEntityQueries
    private val archiveTagQueries = database.archiveTagEntityQueries
    private val archiveCategoryQueries = database.archiveCategoryEntityQueries
    private val archiveAuthorQueries = database.userEntityQueries

    override fun monitorArchives(query: com.tunjid.me.core.model.ArchiveQuery): Flow<List<Archive>> =
        when {
            query.hasContentFilter -> contentFilteredArchives(query)
            else -> archives(query)
        }
            .flatMapLatest { archiveEntities -> archiveEntitiesToArchives(archiveEntities) }
            .distinctUntilChanged()

    override fun monitorArchive(
        kind: ArchiveKind,
        id: ArchiveId
    ): Flow<Archive?> =
        archiveQueries.get(
            id = id.value,
            kind = kind.type
        )
            .asFlow()
            .mapToOneOrNull(context = dispatcher)
            .flatMapLatest { it?.let(::archiveEntityToArchive) ?: flowOf(null) }
            .distinctUntilChanged()

    override suspend fun saveArchives(archives: List<Archive>) {
        archiveAuthorQueries.suspendingTransaction(context = dispatcher) {
            archives.forEach(::saveArchive)
        }
    }

    override suspend fun deleteArchives(ids: List<ArchiveId>) {
        archiveAuthorQueries.suspendingTransaction(context = dispatcher) {
            ids.map(ArchiveId::value).forEach(archiveQueries::delete)
        }
    }

    fun saveArchive(archive: Archive) {
        val userEntity = archive.author.toEntity
        val archiveEntity = archive.toEntity

        archiveAuthorQueries.upsert(
            id = userEntity.id,
            first_name = userEntity.first_name,
            last_name = userEntity.last_name,
            full_name = userEntity.full_name,
            image_url = userEntity.image_url
        )
        archiveQueries.upsert(
            id = archiveEntity.id,
            title = archiveEntity.title,
            description = archiveEntity.description,
            thumbnail = archiveEntity.thumbnail,
            body = archiveEntity.body,
            created = archiveEntity.created,
            link = archiveEntity.link,
            likes = archiveEntity.likes,
            author = userEntity.id,
            kind = archiveEntity.kind,
        )

        archiveTagQueries.delete(archive.id.value)
        archive.tags.forEach { tag ->
            archiveTagQueries.upsert(
                archive_id = archiveEntity.id,
                tag = tag.value,
            )
        }

        archiveCategoryQueries.delete(archive.id.value)
        archive.categories.forEach { category ->
            archiveCategoryQueries.upsert(
                archive_id = archiveEntity.id,
                category = category.value,
            )
        }
    }

    private fun archives(query: com.tunjid.me.core.model.ArchiveQuery): Flow<List<ArchiveEntity>> =
        archiveQueries.find(
            kind = query.kind.type,
            limit = query.limit.toLong(),
            offset = query.offset.toLong()
        )
            .asFlow()
            .mapToList(context = dispatcher)

    private fun contentFilteredArchives(query: com.tunjid.me.core.model.ArchiveQuery): Flow<List<ArchiveEntity>> =
        archiveQueries.idsForQuery(
            kind = query.kind.type,
            limit = query.limit.toLong(),
            offset = query.offset.toLong(),
            tagsOrCategories = query.contentFilter.tags.map(Tag::value)
                .plus(query.contentFilter.categories.map(Category::value))
                .distinct()
        )
            .asFlow()
            .mapToList(context = this.dispatcher)
            .flatMapLatest {
                archiveQueries.archivesForIds(it)
                    .asFlow()
                    .mapToList(context = this.dispatcher)
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
            Archive(
                id = ArchiveId(archiveEntity.id),
                link = archiveEntity.link,
                title = archiveEntity.title,
                description = archiveEntity.description,
                thumbnail = archiveEntity.thumbnail,
                likes = archiveEntity.likes,
                kind = ArchiveKind.values()
                    .first { it.type == archiveEntity.kind },
                created = Instant.fromEpochMilliseconds(archiveEntity.created),
                body = archiveEntity.body,
                author = author.toUser,
                tags = tags.map(com.tunjid.me.core.model.Descriptor::Tag),
                categories = categories.map(com.tunjid.me.core.model.Descriptor::Category),
            )
        }

}
