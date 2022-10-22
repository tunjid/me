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
import com.tunjid.tiler.ListTiler
import com.tunjid.tiler.Tile
import com.tunjid.tiler.tiledList
import com.tunjid.tiler.toTiledList
import com.tunjid.utilities.PivotRequest
import com.tunjid.utilities.pivotWith
import com.tunjid.utilities.toRequests
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
    combine(
        flow = sharedFlow,
        flow2 = sharedFlow.distinctBy(Action.Fetch::gridSize)
            .map { (fetchAction, fetchActionFlow) ->
                fetchAction to fetchActionFlow.map { it.query }
            }
            .flatMapLatest { (gridSize, flow) ->
                flow.pivotWith(pivotRequest(gridSize))
                    .toRequests<ArchiveQuery, List<ArchiveItem>>()
                    .toTiledList(repo.archiveTiler(Tile.Limiter.List { pages -> pages.size > 4 * gridSize }))
            },
        transform = ::FetchResult
    )
}

private fun pivotRequest(gridSize: Int) = PivotRequest<ArchiveQuery>(
    onCount = 3 * gridSize,
    offCount = 1 * gridSize,
    nextQuery = { copy(offset = offset + limit) },
    previousQuery = {
        if (offset == 0) null
        else copy(offset = maxOf(0, offset - limit))
    },
)

private fun ArchiveRepository.archiveTiler(
    limiter: Tile.Limiter.List<ArchiveQuery, List<ArchiveItem>>
): ListTiler<ArchiveQuery, List<ArchiveItem>> =
    tiledList(
        limiter = limiter,
        order = Tile.Order.PivotSorted(comparator = compareBy(ArchiveQuery::offset)),
        fetcher = { query ->
            monitorArchives(query).map<List<Archive>, List<ArchiveItem>> { archives ->
                archives.map { archive ->
                    ArchiveItem.Result(archive = archive, query = query)
                }
            }
                .onStart {
                    emit(listOf(ArchiveItem.Loading(isCircular = false, query = query)))
                }
        }
    )

/**
 * Creates a [Flow] of [Flow] where each inner [Flow] has the same [R]. Changes to [R] emits new inner [Flow]s into
 * the stream.
 */
private fun <T, R> Flow<T>.distinctBy(splitter: (T) -> R): Flow<Pair<R, Flow<T>>> =
    channelFlow channel@{
        var currentKey: R? = null
        var currentFlow = MutableSharedFlow<T>()
        this@distinctBy.collect { item ->
            when (val emittedKey: R = splitter(item)) {
                currentKey -> {
                    currentFlow.subscriptionCount.first { it > 0 }
                    currentFlow.emit(item)
                }

                else -> {
                    currentKey = emittedKey
                    currentFlow = MutableSharedFlow()
                    channel.send(emittedKey to currentFlow.onStart { emit(item) })
                }
            }
        }
    }