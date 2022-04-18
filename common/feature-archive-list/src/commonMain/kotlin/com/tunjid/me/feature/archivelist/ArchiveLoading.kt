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
import com.tunjid.me.core.model.ArchiveQuery
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.tiler.Tile
import com.tunjid.tiler.tiledList
import com.tunjid.tiler.toTiledList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn

data class FetchResult(
    val action: Action.Fetch,
    val queriedArchives: List<List<ArchiveItem>>
)

/**
 * A summary of the loading queries in the app
 */
private data class FetchMetadata(
    // Queries that are actively monitored
    val on: List<ArchiveQuery> = listOf(),
    // Queries that are not being monitored, but whose results are in memory
    val off: List<ArchiveQuery> = listOf(),
    // Queries that will be removed from memory
    val evict: List<ArchiveQuery> = listOf(),
)

private fun FetchMetadata.tileRequests(): Flow<Tile.Input.List<ArchiveQuery, List<ArchiveItem>>> =
    listOf<List<Tile.Input.List<ArchiveQuery, List<ArchiveItem>>>>(
        on.map { Tile.Request.On(it) },
        off.map { Tile.Request.Off(it) },
        evict.map { Tile.Request.Evict(it) },
        listOf(Tile.Limiter.List{ it.size > 40})
    )
        .flatten()
        .asFlow()

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
    combine(
        flow = sharedFlow,
        flow2 = sharedFlow
            .fetchMetadata()
            .flatMapLatest(FetchMetadata::tileRequests)
            .toTiledList(repo.archiveTiler())
            .debounce(150),
        transform = ::FetchResult
    )
}

/**
 * Converts [Action.Fetch] requests to a [Flow] of [FetchMetadata] allowing for efficient pagination dependent on
 * screen size.
 */
private fun Flow<Action.Fetch>.fetchMetadata(): Flow<FetchMetadata> =
    distinctUntilChanged()
        .scan(FetchMetadata()) { existingQueries, fetchAction ->
            val query = fetchAction.query
            val gridSize = fetchAction.gridSize
            val shouldReset = fetchAction is Action.Fetch.Reset
            val on = listOf(
                *query.shift(size = gridSize, operator = Int::minus),
                query,
                *query.shift(size = gridSize, operator = Int::plus),
            )
                .filter { it.offset >= 0 }
            val off = listOf(
                *query.shift(start = gridSize + 1, size = gridSize, operator = Int::minus),
                *query.shift(start = gridSize + 1, size = gridSize, operator = Int::plus),
            )
                .filter { it.offset >= 0 }

            FetchMetadata(
                on = on,
                off = off,
                evict = when {
                    shouldReset -> existingQueries.on + existingQueries.off
                    else -> (existingQueries.on + existingQueries.off) - (on + off).toSet()
                }
            )
        }

private fun ArchiveRepository.archiveTiler(): (Flow<Tile.Input.List<ArchiveQuery, List<ArchiveItem>>>) -> Flow<List<List<ArchiveItem>>> =
    tiledList(
        // Limit results to at most 4 pages at once
        limiter = Tile.Limiter.List { pages -> pages.size > 4 },
        order = Tile.Order.PivotSorted(comparator = compareBy(ArchiveQuery::offset)),
        fetcher = { query ->
            monitorArchives(query).map<List<Archive>, List<ArchiveItem>> { archives ->
                archives.map { archive ->
                    ArchiveItem.Result(
                        archive = archive,
                        query = query
                    )
                }
            }
                .onStart {
                    emit(listOf(ArchiveItem.Loading(isCircular = false, query = query)))
                }
        }
    )

/**
 * Returns an [Array] of [ArchiveQuery] of [size] in which each element has it's [ArchiveQuery.offset] changed by
 * the result of [operator] applied to the [ArchiveQuery.limit] of [this]  at each index.
 *
 * [start] the value applied to [ArchiveQuery.limit] at the first index
 */
private fun ArchiveQuery.shift(
    start: Int = 1,
    size: Int,
    operator: (Int, Int) -> Int
): Array<ArchiveQuery> = Array(size) { index ->
    copy(offset = operator(offset, (limit * (index + start))))
}
