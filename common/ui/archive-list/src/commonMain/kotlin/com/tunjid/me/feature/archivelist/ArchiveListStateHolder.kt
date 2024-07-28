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

// See YouTrack: KTIJ-18375
@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package com.tunjid.me.feature.archivelist

import com.tunjid.me.core.model.ArchiveContentFilter
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveQuery
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.model.hasTheSameFilter
import com.tunjid.me.core.model.minus
import com.tunjid.me.core.model.plus
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.feature.FeatureWhileSubscribed
import com.tunjid.me.feature.archivelist.di.kind
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.di.downcast
import com.tunjid.me.scaffold.di.restoreState
import com.tunjid.me.scaffold.isInPrimaryNavMutations
import com.tunjid.me.scaffold.navigation.NavigationMutation
import com.tunjid.me.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.Tile
import com.tunjid.tiler.queries
import com.tunjid.tiler.toPivotedTileInputs
import com.tunjid.tiler.toTiledList
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.transformWhile
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias ArchiveListStateHolder = ActionStateMutator<Action, StateFlow<State>>

@Inject
class ArchiveListStateHolderCreator(
    creator: (scope: CoroutineScope, savedState: ByteArray?, route: Route) -> ArchiveListStateHolder,
) : ScreenStateHolderCreator by creator.downcast()

/**
 * Manages [State] for [ArchiveListRoute]
 */
@Inject
class ActualArchiveListStateHolder(
    archiveRepository: ArchiveRepository,
    authRepository: AuthRepository,
    byteSerializer: ByteSerializer,
    navStateFlow: StateFlow<MultiStackNav>,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    savedState: ByteArray?,
    @Assisted
    route: Route,
) : ArchiveListStateHolder by scope.actionStateFlowMutator(
    initialState = byteSerializer.restoreState(savedState) ?: State(
        queryState = QueryState(
            currentQuery = ArchiveQuery(kind = route.routeParams.kind),
        )
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    inputs = listOf(
        authRepository.authMutations(),
        navStateFlow.isInPrimaryNavMutations(
            route = route,
            mutation = { copy(isInPrimaryNav = it) }
        ),
    ),
    actionTransform = stateHolder@{ actions ->
        actions.toMutationStream(keySelector = Action::key) {
            when (val action = type()) {
                is Action.Fetch -> action.flow.fetchMutations(
                    stateHolder = this@stateHolder,
                    repo = archiveRepository
                )

                is Action.FilterChanged -> action.flow.filterChangedMutations(
                    repo = archiveRepository,
                    kind = route.routeParams.kind
                )

                is Action.ListStateChanged -> action.flow.listStateChangeMutations()
                is Action.ToggleFilter -> action.flow.filterToggleMutations()
                is Action.Navigate -> action.flow.consumeNavigationActions(
                    navigationMutationConsumer = navActions
                )
            }
        }
    }
)

internal fun AuthRepository.authMutations(): Flow<Mutation<State>> =
    isSignedIn
        .distinctUntilChanged()
        .mapToMutation {
            copy(
                isSignedIn = it,
                hasFetchedAuthStatus = true,
            )
        }

/**
 * Notifies of updates in the archive filter
 */
private fun Flow<Action.FilterChanged>.filterChangedMutations(
    repo: ArchiveRepository,
    kind: ArchiveKind,
): Flow<Mutation<State>> = mapLatestToManyMutations { (descriptor) ->
    // First update the text in the UI
    emit {
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
    // Then asynchronously fetch suggestions
    emitAll(
        repo.descriptorsMatching(
            descriptor = descriptor,
            kind = kind
        ).mapToMutation { descriptors ->
            copy(
                queryState = queryState.copy(
                    suggestedDescriptors = descriptors
                        .filterNot { descriptor ->
                            val contentFilter = queryState.currentQuery.contentFilter
                            when (descriptor) {
                                is Descriptor.Category -> contentFilter.categories.contains(
                                    descriptor
                                )

                                is Descriptor.Tag -> contentFilter.tags.contains(
                                    descriptor
                                )
                            }
                        }
                        .take(10),
                )
            )

        }
    )
}

/**
 * Every toggle isExpanded == null should be processed, however every specific request to
 * expand or collapse, should be distinct until changed.
 */
internal fun Flow<Action.ToggleFilter>.filterToggleMutations(): Flow<Mutation<State>> =
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
        .mapToMutation { isExpanded ->
            copy(queryState = queryState.copy(expanded = isExpanded ?: !queryState.expanded))
        }

/**
 * Saves scroll state across app restarts
 */
private fun Flow<Action.ListStateChanged>.listStateChangeMutations(): Flow<Mutation<State>> =
    distinctUntilChanged()
        .mapToMutation {
            copy(
                savedListState = SavedListState(
                    firstVisibleItemIndex = it.firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = it.firstVisibleItemScrollOffset
                )
            )
        }

/**
 * Converts requests to fetch archives into a list of archives to render
 */
private suspend fun Flow<Action.Fetch>.fetchMutations(
    stateHolder: SuspendingStateHolder<State>,
    repo: ArchiveRepository,
): Flow<Mutation<State>> = scan(
    initial = Pair(
        MutableStateFlow(value = stateHolder.state().queryState.currentQuery),
        MutableStateFlow(value = 1)
    )
) { accumulator, action ->
    val (queries, numColumns) = accumulator
    when (action) {
        is Action.Fetch.LoadAround -> queries.value = queries.value.stabilizeQuery(
            LoadReason.LoadMore(
                action.query
            )
        )

        is Action.Fetch.QueryChange -> queries.value = queries.value.stabilizeQuery(
            action.loadReason(
                state = stateHolder.state(),
                repo = repo
            )
        )

        is Action.Fetch.NoColumnsChanged -> numColumns.value = action.noColumns
    }
    // Emit the same item with each action
    accumulator
}
    // Only emit once
    .distinctUntilChanged()
    // Flatmap to the fields defined earlier
    .flatMapLatest { (queries, numColumns) ->
        val archivesAvailable = queries.flatMapLatest(
            repo::count
        )
        val pivotInputs = queries.toPivotedTileInputs(
            pivotRequests = numColumns
                .map(::pivotRequest)
                .distinctUntilChanged()
        )
        val limitInputs = numColumns.map { noColumns ->
            Tile.Limiter<ArchiveQuery, ArchiveItem.Card>(
                maxQueries = 4 * noColumns,
                itemSizeHint = null,
            )
        }
        val archiveItems = merge(pivotInputs, limitInputs).toTiledList(
            repo.archiveTiler(
                limiter = Tile.Limiter(
                    maxQueries = 4,
                    itemSizeHint = null,
                )
            )
        )
        val listItemMutations: Flow<Mutation<State>> = archiveItems
            .map { stateHolder.state().items to it }
            // To preserve keys, when items are fetched for changes in the query, debounce emissions
            .debounce { (oldItems, newItems) ->
                // new items are still loading, debounce
                if (newItems.isEmpty()) return@debounce QUERY_CHANGE_DEBOUNCE

                val oldQueries = oldItems.queries()
                val newQueries = newItems.queries()
                val isDiffFilter = oldQueries.isNotEmpty() && !newQueries.first()
                    .hasTheSameFilter(oldQueries.first())

                // new items were fetched for a different query and placeholders are present, debounce
                if (isDiffFilter && newItems.hasPlaceholders()) QUERY_CHANGE_DEBOUNCE
                else 0L
            }
            .mapToMutation { (_, newItems) ->
                copy(
                    isLoading = false,
                    listState = listState ?: savedListState.initialListState(),
                    items = preserveKeys(newItems = newItems).itemsWithHeaders,
                )
            }
        val itemsAvailableMutations: Flow<Mutation<State>> = archivesAvailable
            .mapToMutation { count ->
                copy(queryState = queryState.copy(count = count))
            }
        val queryStateMutations: Flow<Mutation<State>> = queries
            .mapToMutation { query ->
                copy(
                    queryState = queryState.copy(
                        currentQuery = query,
                        suggestedDescriptors = queryState.suggestedDescriptors.filterNot { descriptor ->
                            val contentFilter = query.contentFilter
                            when (descriptor) {
                                is Descriptor.Category -> contentFilter.categories.contains(
                                    descriptor
                                )

                                is Descriptor.Tag -> contentFilter.tags.contains(descriptor)
                            }
                        },
                    )
                )
            }
        merge(
            listItemMutations,
            itemsAvailableMutations,
            queryStateMutations
        )
    }

private suspend fun Action.Fetch.QueryChange.loadReason(
    state: State,
    repo: ArchiveRepository
): LoadReason.QueryChange {
    val currentQuery = state.queryState.currentQuery
    val newQuery = when (this) {
        is Action.Fetch.QueryChange.AddDescriptor -> currentQuery + descriptor
        is Action.Fetch.QueryChange.RemoveDescriptor -> currentQuery - descriptor
        Action.Fetch.QueryChange.ClearDescriptors -> currentQuery.copy(
            contentFilter = ArchiveContentFilter()
        )

        Action.Fetch.QueryChange.ToggleOrder -> currentQuery.copy(
            desc = !currentQuery.desc
        )

        is Action.Fetch.QueryChange.ToggleCategory -> when {
            currentQuery.contentFilter.categories.contains(category) -> currentQuery - category
            else -> currentQuery + category
        }
    }
    val firstVisibleItemIndex = state.savedListState.firstVisibleItemIndex
    val visibleOffset = when {
        firstVisibleItemIndex > state.items.lastIndex -> currentQuery.offset
        else -> when (val visibleItem = state.items.subList(
            fromIndex = firstVisibleItemIndex,
            toIndex = state.items.size
        )
            .filterIsInstance<ArchiveItem.Card.Loaded>()
            .firstOrNull()) {
            null -> currentQuery.offset
            else -> (repo.offsetForId(
                id = visibleItem.archive.id,
                query = newQuery
            ).first()).toInt()
        }
    }
    return LoadReason.QueryChange(newQuery.copy(offset = visibleOffset))
}

/**
 * Make sure [ArchiveQuery.offset] is in multiples of [ArchiveQuery.limit] and that load more queries match the
 * current query
 */
private fun ArchiveQuery.stabilizeQuery(
    reason: LoadReason,
): ArchiveQuery {
    val query = when (reason) {
        is LoadReason.QueryChange -> reason.query
        is LoadReason.LoadMore -> when {
            reason.query.hasTheSameFilter(this) -> reason.query
            else -> return this
        }
    }

    val limit = query.limit
    val offset = query.offset
    val modulo = offset % limit
    return query.copy(
        offset = when {
            modulo == 0 -> offset
            (limit - modulo <= limit / 2) -> offset - modulo
            else -> offset + (limit - modulo)
        }
    )
}

private sealed class LoadReason {
    abstract val query: ArchiveQuery

    data class LoadMore(override val query: ArchiveQuery) : LoadReason()
    data class QueryChange(override val query: ArchiveQuery) : LoadReason()
}

private const val QUERY_CHANGE_DEBOUNCE = 300L