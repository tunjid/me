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

package com.tunjid.me.feature.archivelist

import com.tunjid.me.core.model.Archive
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveQuery
import com.tunjid.me.core.model.User
import com.tunjid.me.core.model.UserId
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.tiler.ListTiler
import com.tunjid.tiler.PivotRequest
import com.tunjid.tiler.Tile
import com.tunjid.tiler.listTiler
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

internal fun ArchiveRepository.archiveTiler(
    limiter: Tile.Limiter<ArchiveQuery, ArchiveItem.Card>
): ListTiler<ArchiveQuery, ArchiveItem.Card> =
    listTiler(
        limiter = limiter,
        order = Tile.Order.Sorted(
            comparator = archiveQueryComparator,
        ),
        fetcher = { query ->
            flow {
                val placeholders = (0 until query.limit).map { index ->
                    ArchiveItem.Card.PlaceHolder(
                        index = query.offset + index,
                        key = "${query.offset + index}-${query.hashCode()}",
                        archive = emptyArchive(),
                        isVisible = true
                    )
                }
                emit(placeholders)
                emitAll(
                    archivesStream(query).map { archives ->
                        if (archives.isEmpty()) emptyList()
                        // Ensure a fixed amount of items are returned
                        else placeholders.mapIndexed { index, placeHolder ->
                            when (val archive = archives.getOrNull(index)) {
                                null -> placeHolder.copy(isVisible = false)
                                else -> ArchiveItem.Card.Loaded(
                                    index = query.offset + index,
                                    // Maintain keys between placeholders and loaded items
                                    key = placeholders[index].key,
                                    archive = archive
                                )
                            }
                        }
                    }
                )
            }
        }
    )

internal fun pivotRequest(gridSize: Int) = PivotRequest<ArchiveQuery, ArchiveItem.Card>(
    onCount = 3 * gridSize,
    offCount = 1 * gridSize,
    nextQuery = nextArchiveQuery,
    previousQuery = previousArchiveQuery,
    comparator = archiveQueryComparator,
)

private val archiveQueryComparator = compareBy(ArchiveQuery::offset)

private val nextArchiveQuery: ArchiveQuery.() -> ArchiveQuery? = {
    copy(offset = offset + limit)
}

private val previousArchiveQuery: ArchiveQuery.() -> ArchiveQuery? = {
    if (offset == 0) null
    else copy(
        offset = maxOf(
            a = 0,
            b = offset - limit
        )
    )
}

private fun emptyArchive() = Archive(
    id = ArchiveId(""),
    link = "",
    title = "\n",
    body = "",
    description = "\n",
    thumbnail = null,
    videoUrl = null,
    author = User(
        id = UserId(""),
        firstName = "",
        lastName = "",
        fullName = "",
        imageUrl = "",
    ),
    likes = 0L,
    created = Instant.DISTANT_PAST,
    tags = emptyList(),
    categories = emptyList(),
    kind = ArchiveKind.Articles
)