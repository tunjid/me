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

package com.tunjid.me.common.ui.archivelist

import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.tiler.Tile
import com.tunjid.tiler.tiledList
import com.tunjid.tiler.toTiledList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

data class FetchResult(
    val action: Action.Fetch,
    val queriedArchives: List<List<ArchiveItem>>
)

/**
 * A summary of the loading queries in the app
 */
private data class FetchMetadata(
    val previousQueries: List<com.tunjid.me.core.model.ArchiveQuery> = listOf(),
    val currentQueries: List<com.tunjid.me.core.model.ArchiveQuery> = listOf(),
    val toEvict: List<com.tunjid.me.core.model.ArchiveQuery> = listOf(),
    val inMemory: List<com.tunjid.me.core.model.ArchiveQuery> = listOf(),
)

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
            .flatMapLatest { (previousQueries, currentQueries, evictions) ->
                val toTurnOn = currentQueries
                    .map { Tile.Request.On<com.tunjid.me.core.model.ArchiveQuery, List<ArchiveItem>>(it) }

                val toTurnOff = previousQueries
                    .filterNot { currentQueries.contains(it) }
                    .map { Tile.Request.Off<com.tunjid.me.core.model.ArchiveQuery, List<ArchiveItem>>(it) }

                val toEvict = evictions
                    .map { Tile.Request.Evict<com.tunjid.me.core.model.ArchiveQuery, List<ArchiveItem>>(it) }

                (toTurnOn + toTurnOff + toEvict).asFlow()
            }
            .toTiledList(repo.archiveTiler())
            .debounce(150),
        transform = ::FetchResult
    )
}

/**
 * Converts a query to fetch data to a window to monitor multiple queries concurrently
 */
private fun Flow<Action.Fetch>.fetchMetadata(): Flow<FetchMetadata> =
distinctUntilChanged()
        .scan(FetchMetadata()) { existingQueries, fetchAction ->
        val query = fetchAction.query
        val shouldReset = fetchAction is Action.Fetch.Reset
        val newQueries = listOf(
            query.copy(offset = query.offset - query.limit),
            query.copy(offset = query.offset + query.limit),
            query
        )
            .filter { it.offset >= 0 }

        val currentlyInMemory = (existingQueries.inMemory + newQueries).distinct()

        // Evict all queries in memory if we're resetting, else trim for against memory pressure
        val toEvict = if (shouldReset) existingQueries.inMemory else when (val min =
            newQueries.minByOrNull(com.tunjid.me.core.model.ArchiveQuery::offset)) {
            null -> listOf()
            // Evict items more than 3 offset pages behind the min current query
            else -> currentlyInMemory.filter {
                it.offset - min.offset < -(com.tunjid.me.core.model.DefaultQueryLimit * 3)
            }
        }
        existingQueries.copy(
            previousQueries = if (shouldReset) listOf() else existingQueries.currentQueries,
            currentQueries = newQueries,
            inMemory = currentlyInMemory - toEvict.toSet(),
            toEvict = toEvict
        )
    }

private fun ArchiveRepository.archiveTiler(): (Flow<Tile.Input.List<com.tunjid.me.core.model.ArchiveQuery, List<ArchiveItem>>>) -> Flow<List<List<ArchiveItem>>> =
    tiledList(
        // Limit results to at most 4 pages at once
        limiter = Tile.Limiter.List { pages -> pages.size > 4 },
        order = Tile.Order.PivotSorted(comparator = compareBy(com.tunjid.me.core.model.ArchiveQuery::offset)),
        fetcher = { query ->
            monitorArchives(query).map<List<com.tunjid.me.core.model.Archive>, List<ArchiveItem>> { archives ->
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

