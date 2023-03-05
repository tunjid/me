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

import com.tunjid.me.core.model.ArchiveQuery
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.tiler.ListTiler
import com.tunjid.tiler.Tile
import com.tunjid.tiler.listTiler
import com.tunjid.tiler.utilities.PivotRequest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal fun ArchiveRepository.archiveTiler(
    limiter: Tile.Limiter<ArchiveQuery, ArchiveItem>
): ListTiler<ArchiveQuery, ArchiveItem> =
    listTiler(
        limiter = limiter,
        order = Tile.Order.Sorted(
            comparator = archiveQueryComparator,
        ),
        fetcher = { query ->
            flow {
                emit(
                    (0 until query.limit)
                        .map {
                            ArchiveItem.Loading(
                                index = query.offset + it,
                                queryId = query.hashCode(),
                                isCircular = false
                            )
                        }
                )
                emitAll(
                    archivesStream(query).map { archives ->
                        archives.mapIndexed { index, archive ->
                            ArchiveItem.Loaded(
                                index = query.offset + index,
                                queryId = query.hashCode(),
                                archive = archive
                            )
                        }
                    }
                )
            }
        }
    )

internal fun pivotRequest(gridSize: Int) = PivotRequest(
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
