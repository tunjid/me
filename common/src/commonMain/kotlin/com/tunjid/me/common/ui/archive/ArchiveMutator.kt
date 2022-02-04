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
import com.tunjid.me.common.data.ByteSerializable
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
import com.tunjid.tiler.tiledList
import com.tunjid.tiler.toTiledList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

typealias ArchiveMutator = Mutator<Action, StateFlow<State>>

@Serializable
data class State(
    val gridSize: Int = 1,
    val shouldScrollToTop: Boolean = true,
    val isInNavRail: Boolean = false,
    val queryState: QueryState,
    @Transient
    val lastVisibleKey: Any? = null,
    @Transient
    val items: List<ArchiveItem> = listOf()
) : ByteSerializable

sealed class Action(val key: String) {
    sealed class Fetch : Action(key = "Fetch") {
        abstract val query: ArchiveQuery

        data class Reset(
            override val query: ArchiveQuery
        ) : Fetch()

        data class LoadMore(
            override val query: ArchiveQuery
        ) : Fetch()
    }

    data class FilterChanged(
        val descriptor: Descriptor
    ) : Action(key = "FilterChanged")

    data class Navigate(
        val navAction: AppAction.Nav
    ) : Action(key = "Navigate")

    data class GridSize(val size: Int) : Action(key = "GridSize")

    data class ToggleFilter(val isExpanded: Boolean? = null) : Action(key = "ToggleFilter")

    data class LastVisibleKey(val itemKey: Any) : Action(key = "LastVisibleKey")
}

sealed class ArchiveItem {
    abstract val query: ArchiveQuery

    data class Result(
        val archive: Archive,
        override val query: ArchiveQuery,
    ) : ArchiveItem()

    data class Loading(
        val isCircular: Boolean,
        override val query: ArchiveQuery,
    ) : ArchiveItem()
}

private class ItemKey(
    val key: String,
    val query: ArchiveQuery
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ItemKey

        if (key != other.key) return false
        if (query != other.query) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + query.hashCode()
        return result
    }
}

val ArchiveItem.key: Any
    get() = when (this) {
        is ArchiveItem.Loading -> ItemKey(
            key = "header-$query",
            query = query
        )
        is ArchiveItem.Result -> ItemKey(
            key = "result-${archive.id}",
            query = query
        )
    }

val Any.queryFromKey get() = if(this is ItemKey) this.query else null

val ArchiveItem.Result.prettyDate: String
    get() {
        val dateTime = archive.created.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dateTime.month.name} ${dateTime.monthNumber} ${dateTime.year}"
    }

val ArchiveItem.Result.readTime get() = "${archive.body.trim().split("/\\s+/").size / 250} min read"

@Serializable
data class QueryState(
    val expanded: Boolean = false,
    val startQuery: ArchiveQuery,
    val currentQuery: ArchiveQuery,
    val categoryText: Descriptor.Category = Descriptor.Category(""),
    val tagText: Descriptor.Tag = Descriptor.Tag(""),
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

private val FetchResult.flattenedArchives get() = queriedArchives
    .flatten()
    .distinctBy { it.key }

private val FetchResult.hasNoResults
    get() = queriedArchives.isEmpty() || queriedArchives.all {
        it.all { items -> items is ArchiveItem.Loading }
    }

fun archiveMutator(
    scope: CoroutineScope,
    route: ArchiveRoute,
    initialState: State? = null,
    repo: ArchiveRepository,
    appMutator: AppMutator,
): ArchiveMutator = stateFlowMutator(
    scope = scope,
    initialState = initialState ?: State(
        items = listOf(
            ArchiveItem.Loading(
                isCircular = true,
                query = route.query
            )
        ),
        queryState = QueryState(
            startQuery = route.query,
            currentQuery = route.query,
        )
    ),
    started = SharingStarted.WhileSubscribed(),
    actionTransform = { actions ->
        merge(
            appMutator.navRailStatusMutations(),
            actions.toMutationStream(keySelector = Action::key) {
                when (val action = type()) {
                    is Action.Fetch -> action.flow.fetchMutations(scope = scope, repo = repo)
                    is Action.Navigate -> action.flow.map { it.navAction }.consumeWith(appMutator)
                    is Action.FilterChanged -> action.flow.filterChangedMutations()
                    is Action.ToggleFilter -> action.flow.filterToggleMutations()
                    is Action.LastVisibleKey -> action.flow.resetScrollMutations()
                    is Action.GridSize -> action.flow.gridSizeMutations()
                }
            }
        ).monitorWhenActive(appMutator)
    }
)

private fun AppMutator.navRailStatusMutations() = state.map { appState ->
    appState.nav.navRailRoute is ArchiveRoute && appState.ui.navRailVisible
}
    .distinctUntilChanged()
    .map {
        Mutation<State> { copy(isInNavRail = it) }
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

/**
 * Every toggle isExpanded == null should be processed, however every specific request to
 * expand or collapse, should be distinct until changed.
 */
private fun Flow<Action.ToggleFilter>.filterToggleMutations(): Flow<Mutation<State>> =
    map { it.isExpanded }
        .scan(listOf<Boolean?>()) { emissions, isExpanded -> (emissions + isExpanded).takeLast(2) }
        .transformWhile { emissions ->
            when {
                emissions.isEmpty() -> Unit
                emissions.size == 1 -> emit(emissions.first())
                else -> {
                    val (previous, current) = emissions
                    if (current == null || current != previous) emit(current)
                }
            }
            true
        }
        .map { isExpanded ->
            Mutation {
                copy(queryState = queryState.copy(expanded = isExpanded ?: !queryState.expanded))
            }
        }

private fun Flow<Action.GridSize>.gridSizeMutations(): Flow<Mutation<State>> =
    distinctUntilChanged()
        .map {
            Mutation {
                copy(gridSize = it.size)
            }
        }

private fun Flow<Action.LastVisibleKey>.resetScrollMutations(): Flow<Mutation<State>> =
    distinctUntilChanged()
        .map {
            Mutation {
                copy(lastVisibleKey = it.itemKey)
            }
        }

private fun Flow<Action.Fetch>.fetchMutations(
    scope: CoroutineScope,
    repo: ArchiveRepository
): Flow<Mutation<State>> = toFetchResult(
    scope = scope,
    repo = repo
)
    .map { fetchResult ->
        Mutation {
            val fetchAction = fetchResult.action
            val items = when {
                fetchResult.hasNoResults -> when (fetchAction) {
                    // Fetch action is reset, show a loading spinner
                    is Action.Fetch.Reset -> listOf(
                        ArchiveItem.Loading(
                            isCircular = true,
                            query = fetchAction.query
                        )
                    )
                    // The mutator was just resubscribed to, show existing items
                    else -> items
                }
                else -> fetchResult.flattenedArchives
            }
                // Filtering is cheap because at most 4 * [DefaultQueryLimit] items
                // are ever sent to the UI
                .filter { item ->
                    when (item) {
                        is ArchiveItem.Loading -> true
                        is ArchiveItem.Result -> item.query.contentFilter == fetchAction.query.contentFilter
                    }
                }
            copy(
                items = items,
                queryState = queryState.copy(
                    currentQuery = fetchResult.action.query,
                    startQuery = when (fetchAction) {
                        is Action.Fetch.Reset -> queryState.startQuery.copy(
                            contentFilter = fetchAction.query.contentFilter
                        )
                        else -> queryState.startQuery
                    },
                    expanded = when (fetchAction) {
                        is Action.Fetch.Reset -> true
                        else -> queryState.expanded
                    }
                )
            )
        }
    }

private fun ArchiveRepository.archiveTiler(): (Flow<Input.List<ArchiveQuery, List<ArchiveItem>>>) -> Flow<List<List<ArchiveItem>>> =
    tiledList(
        // Limit results to at most 4 pages at once
        limiter = Tile.Limiter.List { pages -> pages.size > 4 },
        order = Tile.Order.PivotSorted(comparator = compareBy(ArchiveQuery::offset)),
        fetcher = { query ->
            monitorArchives(query).map<List<Archive>, List<ArchiveItem>> { archives ->
                delay(3000)
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

private fun Flow<Action.Fetch>.toFetchResult(
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
                    .map { Tile.Request.On<ArchiveQuery, List<ArchiveItem>>(it) }

                val toTurnOff = previousQueries
                    .filterNot { currentQueries.contains(it) }
                    .map { Tile.Request.Off<ArchiveQuery, List<ArchiveItem>>(it) }

                val toEvict = evictions
                    .map { Tile.Request.Evict<ArchiveQuery, List<ArchiveItem>>(it) }

                (toTurnOn + toTurnOff + toEvict).asFlow()
            }
            .toTiledList(repo.archiveTiler())
            .debounce(150),
        transform = ::FetchResult
    )
}

private fun Flow<Action.Fetch>.fetchMetadata(): Flow<FetchMetadata> =
    scan(FetchMetadata()) { existingQueries, fetchAction ->
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
            newQueries.minByOrNull(ArchiveQuery::offset)) {
            null -> listOf()
            // Evict items more than 3 offset pages behind the min current query
            else -> currentlyInMemory.filter {
                it.offset - min.offset < -(DefaultQueryLimit * 3)
            }
        }
        existingQueries.copy(
            previousQueries = if (shouldReset) listOf() else existingQueries.currentQueries,
            currentQueries = newQueries,
            inMemory = currentlyInMemory - toEvict.toSet(),
            toEvict = toEvict
        )
    }
