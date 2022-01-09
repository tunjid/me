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

import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.TransactionWithoutReturn
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import com.tunjid.me.common.data.Api
import com.tunjid.me.common.data.AppDatabase
import com.tunjid.me.common.data.ArchiveEntity
import com.tunjid.me.common.data.UserEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface ArchiveRepository {
    fun monitorArchives(query: ArchiveQuery): Flow<List<Archive>>
    fun monitorArchive(kind: ArchiveKind, id: String): Flow<Archive>
}

class ReactiveArchiveRepository(
    private val api: Api,
    database: AppDatabase,
    dispatcher: CoroutineDispatcher,
) : ArchiveRepository {

    private val localArchiveRepository = LocalArchiveRepository(
        database,
        dispatcher
    )

    override fun monitorArchives(query: ArchiveQuery): Flow<List<Archive>> =
        localArchiveRepository.monitorArchives(query)
            .transformWhile { archives ->
                emit(archives)
                // Oof nothing in the DB, fetch it!
                if (archives.isEmpty()) localArchiveRepository.saveArchives(
                    archives = fetchArchives(query)
                )
                true
            }

    override fun monitorArchive(kind: ArchiveKind, id: String): Flow<Archive> = flow {
        emit(api.fetchArchive(kind = kind, id = id))
    }

    private suspend fun fetchArchive(kind: ArchiveKind, id: String): Archive =
        api.fetchArchive(kind = kind, id = id)

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

private class LocalArchiveRepository(
    database: AppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : ArchiveRepository {

    private val archiveQueries = database.archiveEntityQueries
    private val archiveTagQueries = database.archiveTagEntityQueries
    private val archiveCategoryQueries = database.archiveCategoryEntityQueries
    private val archiveAuthorQueries = database.userEntityQueries

    override fun monitorArchives(query: ArchiveQuery): Flow<List<Archive>> =
        archiveQueries.find(
            kind = query.kind.type,
            limit = query.limit.toLong(),
            offset = query.offset.toLong()
        )
            .asFlow()
            .mapToList(context = dispatcher)
            .flatMapLatest { archiveEntities -> archiveEntities.toArchivesFlow }
            .onEach { println("BIG OUT. Size: ${it.size} for ${query.offset}") }

    override fun monitorArchive(kind: ArchiveKind, id: String): Flow<Archive> =
        archiveQueries.get(id = id)
            .asFlow()
            .mapToOne(context = dispatcher)
            .flatMapLatest { it.toArchiveFlow }

    suspend fun saveArchives(
        archives: List<Archive>
    ) = archiveAuthorQueries.suspendingTransaction(context = dispatcher) {
        archives
            .map { archive ->
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
                    author = userEntity.id,
                    kind = archiveEntity.kind,
                )
                archive.tags.forEach { tag ->
                    archiveTagQueries.upsert(
                        archive_id = archiveEntity.id,
                        tag = tag,
                    )
                }
                archive.categories.forEach { category ->
                    archiveCategoryQueries.upsert(
                        archive_id = archiveEntity.id,
                        category = category,
                    )
                }
            }
    }

    private val List<ArchiveEntity>.toArchivesFlow: Flow<List<Archive>>
        get() = if (isEmpty()) flowOf(listOf()) else combine(
            flows = map { it.toArchiveFlow },
            transform = Array<Archive>::toList
        )

    private val ArchiveEntity.toArchiveFlow: Flow<Archive>
        get() = let { archiveEntity ->
            combine(
                flow = archiveTagQueries.find(archive_id = archiveEntity.id)
                    .asFlow()
                    .mapToList(context = dispatcher),
                flow2 = archiveCategoryQueries.find(archive_id = archiveEntity.id)
                    .asFlow()
                    .mapToList(context = dispatcher),
                flow3 = archiveAuthorQueries.find(id = archiveEntity.author)
                    .asFlow()
                    .mapToOne(context = dispatcher),
            ) { tags, categories, author ->
                Archive(
                    id = archiveEntity.id,
                    link = archiveEntity.link,
                    title = archiveEntity.title,
                    description = archiveEntity.description,
                    thumbnail = archiveEntity.thumbnail,
                    kind = ArchiveKind.values().first { it.type == archiveEntity.kind },
                    created = Instant.fromEpochMilliseconds(archiveEntity.created),
                    body = archiveEntity.body,
                    author = author.toUser,
                    tags = tags,
                    categories = categories,
                )
            }
        }

}

private val UserEntity.toUser
    get() = User(
        id = id,
        firstName = first_name,
        lastName = last_name,
        fullName = full_name,
        imageUrl = image_url,
    )

private val User.toEntity
    get() = UserEntity(
        id = id,
        first_name = firstName,
        last_name = lastName,
        full_name = fullName,
        image_url = imageUrl,
    )

private val Archive.toEntity
    get() = ArchiveEntity(
        id = id,
        body = body,
        thumbnail = thumbnail,
        description = description,
        title = title,
        author = author.id,
        created = created.toEpochMilliseconds(),
        kind = kind.type,
        link = link
    )

private suspend fun Transacter.suspendingTransaction(
    context: CoroutineContext,
    body: TransactionWithoutReturn.() -> Unit
) = withContext(context) {
    suspendCoroutine<Boolean> { continuation ->
        transaction {
            afterRollback { continuation.resume(true) }
            afterCommit { continuation.resume(false) }
            body()
        }
    }
}