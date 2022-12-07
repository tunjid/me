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
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveQuery
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.tiler.ListTiler
import com.tunjid.tiler.Tile
import com.tunjid.tiler.listTiler
import com.tunjid.tiler.toTiledList
import com.tunjid.tiler.utilities.PivotRequest
import com.tunjid.tiler.utilities.pivotWith
import com.tunjid.tiler.utilities.toTileInputs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

/**
 * Converts a query to output data for [State]
 */
fun Flow<Action.Fetch>.toFetchResult(
    scope: CoroutineScope,
    repo: ArchiveRepository
): Flow<FetchResult> = shareIn(
    scope = scope,
    started = SharingStarted.WhileSubscribed(),
    replay = 1
).let { sharedFlow ->
    val queries = sharedFlow
        .map { it.query }
        .distinctUntilChanged()

    val pivotRequests = sharedFlow
        .map { it.gridSize }
        .map { pivotRequest(it) }
        .distinctUntilChanged()

    val limiters = sharedFlow
        .distinctUntilChangedBy { it.gridSize }
        .map { query ->
            Tile.Limiter<ArchiveQuery, ArchiveItem> { items ->
                items.size > 4 * query.gridSize * query.query.limit
            }
        }

    combine(
        flow = sharedFlow,
        flow2 = merge(
            queries.pivotWith(pivotRequests).toTileInputs(),
            limiters
        )
            .toTiledList(
                repo.archiveTiler(
                    kind = ArchiveKind.Articles,
                    limiter = Tile.Limiter { items -> items.size > 100 }
                )
            )
            // Allow database queries to settle
            .debounce(150),
        transform = ::FetchResult
    )
}

private fun pivotRequest(gridSize: Int) = PivotRequest<ArchiveQuery>(
    onCount = 3 * gridSize,
    offCount = 1 * gridSize,
    nextQuery = nextArchiveQuery,
    previousQuery = previousArchiveQuery,
)

private val nextArchiveQuery: ArchiveQuery.() -> ArchiveQuery? = {
    copy(offset = offset + limit)
}

private val previousArchiveQuery: ArchiveQuery.() -> ArchiveQuery? = {
    if (offset == 0) null
    else copy(offset = maxOf(0, offset - limit))
}

private fun ArchiveRepository.archiveTiler(
    kind: ArchiveKind,
    limiter: Tile.Limiter<ArchiveQuery, ArchiveItem>
): ListTiler<ArchiveQuery, ArchiveItem> =
    listTiler(
        limiter = limiter,
        order = Tile.Order.PivotSorted(
            query = ArchiveQuery(kind),
            comparator = compareBy(ArchiveQuery::offset)
        ),
        fetcher = { query ->
            archivesStream(query).map<List<Archive>, List<ArchiveItem>> { archives ->
                archives.map(ArchiveItem::Result)
            }
                .onStart {
                    emit(listOf(ArchiveItem.Loading(isCircular = false)))
                }
        }
    )
