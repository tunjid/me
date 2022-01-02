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

package com.tunjid.me.common.ui.archive

import com.tunjid.me.common.AppAction
import com.tunjid.me.common.AppMutator
import com.tunjid.me.common.consumeWith
import com.tunjid.me.common.data.archive.Archive
import com.tunjid.me.common.data.archive.ArchiveQuery
import com.tunjid.me.common.data.archive.ArchiveRepository
import com.tunjid.me.common.data.archive.DefaultQueryLimit
import com.tunjid.me.common.data.archive.Descriptor
import com.tunjid.me.common.globalui.navRailVisible
import com.tunjid.me.common.monitorWhenActive
import com.tunjid.me.common.nav.navRailRoute
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.Tile
import com.tunjid.tiler.Tile.Input
import com.tunjid.tiler.flattenWith
import com.tunjid.tiler.tiledList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

typealias ArchiveMutator = Mutator<Action, StateFlow<State>>

data class State(
    val gridSize: Int = 1,
    val isInNavRail: Boolean = false,
    val queryState: QueryState,
    val listStateSummary: ListState = ListState(),
    val items: List<ArchiveItem> = listOf(ArchiveItem.Loading)
)

val State.chunkedItems get() = items.chunked(if (isInNavRail) 1 else gridSize)

sealed class Action {
    data class Fetch(
        val query: ArchiveQuery,
        val reset: Boolean = false
    ) : Action()

    data class UpdateListState(val listState: ListState) : Action()

    data class FilterChanged(
        val descriptor: Descriptor
    ) : Action()

    data class Navigate(
        val navAction: AppAction.Nav
    ) : Action()

    data class GridSize(val size: Int) : Action()

    data class ToggleFilter(val isExpanded: Boolean? = null) : Action()
}

sealed class ArchiveItem {
    data class Result(
        val archive: Archive,
        val query: ArchiveQuery,
    ) : ArchiveItem()

    object Loading : ArchiveItem()
}

val ArchiveItem.key: String
    get() = when (this) {
        ArchiveItem.Loading -> "Loading"
        is ArchiveItem.Result -> archive.key
    }

val ArchiveItem.Result.prettyDate: String
    get() {
        val dateTime = archive.created.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dateTime.dayOfWeek} ${dateTime.monthNumber} ${dateTime.year}"
    }

data class QueryState(
    val expanded: Boolean = false,
    val rootQuery: ArchiveQuery,
    val categoryText: Descriptor.Category = Descriptor.Category(""),
    val tagText: Descriptor.Tag = Descriptor.Tag(""),
)

data class ListState(
    val firstVisibleItemIndex: Int = 0,
    val firstVisibleItemScrollOffset: Int = 0
)

/**
 * A summary of the loading queries in the app
 */
private data class FetchMetadata(
    val previousQueries: List<ArchiveQuery> = listOf(),
    val currentQueries: List<ArchiveQuery> = listOf(),
    val toEvict: List<ArchiveQuery> = listOf(),
    val inMemory: List<ArchiveQuery> = listOf(),
)

private data class FetchResult(
    val action: Action.Fetch,
    val queriedArchives: List<List<ArchiveItem>>
)

private val FetchResult.flattenedArchives get() = queriedArchives.flatten()

private val FetchResult.hasNoResults get() = queriedArchives.isEmpty()

fun archiveMutator(
    scope: CoroutineScope,
    route: ArchiveRoute,
    repo: ArchiveRepository,
    appMutator: AppMutator,
): Mutator<Action, StateFlow<State>> = stateFlowMutator(
    scope = scope,
    initialState = State(
        queryState = QueryState(
            rootQuery = route.query,
        )
    ),
    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 2000),
    transform = { actions ->
        appMutator.monitorWhenActive(merge(
            appMutator.navRailStatusMutations(),
            actions.toMutationStream {
                when (val action = type()) {
                    is Action.Fetch -> action.flow.fetchMutations(repo = repo)
                    is Action.Navigate -> action.flow.map { it.navAction }.consumeWith(appMutator)
                    is Action.UpdateListState -> action.flow.updateListStateMutations()
                    is Action.FilterChanged -> action.flow.filterChangedMutations()
                    is Action.ToggleFilter -> action.flow.filterToggleMutations()
                    is Action.GridSize -> action.flow.gridSizeMutations()
                }
            }
        ))
    }
)

private fun AppMutator.navRailStatusMutations() = state.map { appState ->
    appState.nav.navRailRoute is ArchiveRoute && appState.ui.navRailVisible
}
    .distinctUntilChanged()
    .map {
        Mutation<State> { copy(isInNavRail = it) }
    }

private fun Flow<Action.UpdateListState>.updateListStateMutations(): Flow<Mutation<State>> =
    map { (listState) ->
        Mutation { copy(listStateSummary = listState) }
    }

private fun Flow<Action.FilterChanged>.filterChangedMutations(): Flow<Mutation<State>> =
    map { (descriptor) ->
        Mutation {
            copy(
                queryState = queryState.copy(
                    categoryText = when (descriptor) {
                        is Descriptor.Category -> descriptor
                        else -> queryState.categoryText
                    },
                    tagText = when (descriptor) {
                        is Descriptor.Tag -> descriptor
                        else -> queryState.tagText
                    },
                )
            )
        }
    }

private fun Flow<Action.ToggleFilter>.filterToggleMutations(): Flow<Mutation<State>> =
    distinctUntilChanged()
        .map { (isExpanded) ->
            Mutation {
                copy(queryState = queryState.copy(expanded = isExpanded ?: !queryState.expanded))
            }
        }

private fun Flow<Action.GridSize>.gridSizeMutations(): Flow<Mutation<State>> = map {
    Mutation {
        copy(gridSize = it.size)
    }
}

private fun Flow<Action.Fetch>.fetchMutations(repo: ArchiveRepository): Flow<Mutation<State>> =
    toFetchResult(repo = repo)
        .map { fetchResult ->
            Mutation {
                val fetchAction = fetchResult.action
                val items = when {
                    fetchResult.hasNoResults -> when {
                        // Fetch action is reset, show a loading spinner
                        fetchAction.reset -> listOf(ArchiveItem.Loading)
                        // The mutator was just resubscribed to, show existing items
                        else -> items
                    }
                    else -> fetchResult.flattenedArchives
                }
                    // Filtering is cheap because at most 4 * [DefaultQueryLimit] items
                    // are ever sent to the UI
                    .filter { item ->
                        when (item) {
                            ArchiveItem.Loading -> true
                            is ArchiveItem.Result -> item.query.contentFilter == fetchAction.query.contentFilter
                        }
                    }
                copy(
                    items = items,
                    queryState = queryState.copy(
                        rootQuery = when {
                            fetchAction.reset -> queryState.rootQuery.copy(
                                contentFilter = fetchAction.query.contentFilter
                            )
                            else -> queryState.rootQuery
                        },
                        expanded = when {
                            fetchAction.reset -> true
                            else -> queryState.expanded
                        }
                    )
                )
            }
        }

private fun ArchiveRepository.archiveTiler(): (Flow<Input<ArchiveQuery, List<ArchiveItem>>>) -> Flow<List<List<ArchiveItem>>> =
    tiledList(
        flattener = Tile.Flattener.PivotSorted(
            comparator = compareBy(ArchiveQuery::offset),
            // Limit results to at most 4 pages at once
            limiter = { pages -> pages.size > 4 }
        ),
        fetcher = { query ->
            monitorArchives(query).map { archives ->
                archives.map { archive ->
                    ArchiveItem.Result(
                        archive = archive,
                        query = query
                    )
                }
            }
        }
    )

private fun Flow<Action.Fetch>.toFetchResult(repo: ArchiveRepository): Flow<FetchResult> =
    combine(
        flow = this@toFetchResult,
        flow2 = fetchMetadata().flatMapLatest { (previousQueries, currentQueries, evictions) ->
            val toTurnOn = currentQueries
                .map { Tile.Request.On<ArchiveQuery, List<ArchiveItem>>(it) }

            val toTurnOff = previousQueries
                .filterNot { currentQueries.contains(it) }
                .map { Tile.Request.Off<ArchiveQuery, List<ArchiveItem>>(it) }

            val toEvict = evictions
                .map { Tile.Request.Evict<ArchiveQuery, List<ArchiveItem>>(it) }

            (toTurnOn + toTurnOff + toEvict).asFlow()
        }
            .flattenWith(repo.archiveTiler()),
        transform = ::FetchResult
    )

private fun Flow<Action.Fetch>.fetchMetadata(): Flow<FetchMetadata> =
    map { (query, shouldReset) ->
        shouldReset to listOf(
            query.copy(offset = query.offset - query.limit),
            query.copy(offset = query.offset + query.limit),
            query
        )
            .filter { it.offset >= 0 }
    }
        .scan(FetchMetadata()) { existingQueries, (shouldReset, currentQueries) ->
            val currentlyInMemory = (existingQueries.inMemory + currentQueries).distinct()
            val toEvict = if (shouldReset) existingQueries.inMemory else when (val min =
                currentQueries.minByOrNull(ArchiveQuery::offset)) {
                null -> listOf()
                // Evict items more than 3 offset pages behind the min current query
                else -> currentlyInMemory.filter {
                    it.offset - min.offset < -(DefaultQueryLimit * 3)
                }
            }
            existingQueries.copy(
                previousQueries = if (shouldReset) listOf() else existingQueries.currentQueries,
                currentQueries = currentQueries,
                inMemory = currentlyInMemory - toEvict.toSet(),
                toEvict = toEvict
            )
        }
