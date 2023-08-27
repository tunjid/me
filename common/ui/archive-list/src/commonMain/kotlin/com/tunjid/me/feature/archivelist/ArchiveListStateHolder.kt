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

import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveQuery
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.model.hasTheSameFilter
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.feature.FeatureWhileSubscribed
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.di.downcast
import com.tunjid.me.scaffold.di.restoreState
import com.tunjid.me.scaffold.isInMainNavMutations
import com.tunjid.me.scaffold.nav.NavMutation
import com.tunjid.me.scaffold.nav.NavState
import com.tunjid.me.scaffold.nav.consumeNavActions
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.mutator.mutation
import com.tunjid.tiler.Tile
import com.tunjid.tiler.toPivotedTileInputs
import com.tunjid.tiler.toTiledList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformWhile
import me.tatarka.inject.annotations.Inject

typealias ArchiveListStateHolder = ActionStateProducer<Action, StateFlow<State>>

@Inject
class ArchiveListStateHolderCreator(
    creator: (scope: CoroutineScope, savedState: ByteArray?, route: ArchiveListRoute) -> ArchiveListStateHolder,
) : ScreenStateHolderCreator by creator.downcast()

/**
 * Manages [State] for [ArchiveListRoute]
 */
@Inject
class ActualArchiveListStateHolder(
    archiveRepository: ArchiveRepository,
    authRepository: AuthRepository,
    byteSerializer: ByteSerializer,
    navStateFlow: StateFlow<NavState>,
    navActions: (NavMutation) -> Unit,
    scope: CoroutineScope,
    savedState: ByteArray?,
    route: ArchiveListRoute,
) : ArchiveListStateHolder by scope.actionStateFlowProducer(
    initialState = byteSerializer.restoreState(savedState) ?: State(
        queryState = QueryState(
            currentQuery = ArchiveQuery(kind = route.kind),
        )
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    mutationFlows = listOf(
        authRepository.authMutations(),
        navStateFlow.isInMainNavMutations(
            route = route,
            mutation = { copy(isInMainNav = it) }
        ),
    ),
    actionTransform = stateHolder@{ actions ->
        actions.toMutationStream(keySelector = Action::key) {
            when (val action = type()) {
                is Action.Fetch -> action.flow.fetchMutations(
                    scope = scope,
                    state = this@stateHolder,
                    repo = archiveRepository
                )

                is Action.FilterChanged -> action.flow.filterChangedMutations(
                    repo = archiveRepository,
                    kind = route.kind
                )

                is Action.ListStateChanged -> action.flow.listStateChangeMutations()
                is Action.ToggleFilter -> action.flow.filterToggleMutations()
                is Action.Navigate -> action.flow.consumeNavActions(
                    mutationMapper = Action.Navigate::navMutation,
                    action = navActions
                )
            }
        }
    }
)

internal fun AuthRepository.authMutations(): Flow<Mutation<State>> =
    isSignedIn
        .distinctUntilChanged()
        .map {
            mutation {
                copy(
                    isSignedIn = it,
                    hasFetchedAuthStatus = true,
                )
            }
        }

/**
 * Notifies of updates in the archive filter
 */
private fun Flow<Action.FilterChanged>.filterChangedMutations(
    repo: ArchiveRepository,
    kind: ArchiveKind,
): Flow<Mutation<State>> =
    flatMapLatest { (descriptor) ->
        flow {
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
                ).map { descriptors ->
                    mutation {
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
                }
            )
        }
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
        .map { isExpanded ->
            mutation {
                copy(queryState = queryState.copy(expanded = isExpanded ?: !queryState.expanded))
            }
        }

/**
 * Saves scroll state across app restarts
 */
private fun Flow<Action.ListStateChanged>.listStateChangeMutations(): Flow<Mutation<State>> =
    distinctUntilChanged()
        .map {
            mutation {
                copy(
                    savedListState = SavedListState(
                        firstVisibleItemIndex = it.firstVisibleItemIndex,
                        firstVisibleItemScrollOffset = it.firstVisibleItemScrollOffset
                    )
                )
            }
        }

/**
 * Converts requests to fetch archives into a list of archives to render
 */
private fun Flow<Action.Fetch>.fetchMutations(
    scope: CoroutineScope,
    state: SuspendingStateHolder<State>,
    repo: ArchiveRepository,
): Flow<Mutation<State>> {
    val queries = filterIsInstance<Action.Fetch.QueriedFetch>()
        .distinctUntilChanged()
        .scan(null, ArchiveQuery?::stabilizeQuery)
        .filterNotNull()
        .distinctUntilChanged()
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1
        )

    val columnChanges = filterIsInstance<Action.Fetch.NoColumnsChanged>()
        .onStart { emit(Action.Fetch.NoColumnsChanged(1)) }
        .distinctUntilChanged()
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(),
        )

    val archivesAvailable = queries
        .flatMapLatest(repo::count)

    val pivotInputs = queries.toPivotedTileInputs(
        pivotRequests = columnChanges
            .map { it.noColumns }
            .map(::pivotRequest)
            .distinctUntilChanged()
    )

    val limitInputs = columnChanges
        .map { columnChange ->
            Tile.Limiter<ArchiveQuery, ArchiveItem.Card>(
                maxQueries = 4 * columnChange.noColumns,
                itemSizeHint = null,
            )
        }

    val archiveItems = merge(
        pivotInputs,
        limitInputs
    )
        .toTiledList(
            repo.archiveTiler(
                limiter = Tile.Limiter(
                    maxQueries = 4,
                    itemSizeHint = null,
                )
            )
        )

    return merge(
        archiveItems
            .map { state.state().items to it }
            .debounce { (oldItems, newItems) ->
                val oldQueries = (0 until oldItems.tileCount).map(oldItems::queryAtTile)
                val newQueries = (0 until newItems.tileCount).map(newItems::queryAtTile)
                val itemsAtStartIndices = (0 until newItems.tileCount).map { newItems[newItems.tileAt(it).start] }

                val newItemsEmpty = newItems.isEmpty()
                val isDiffFilter = !newItemsEmpty
                        && oldQueries.isNotEmpty()
                        && !newQueries.first().hasTheSameFilter(oldQueries.first())
                val hasPlaceHolder = itemsAtStartIndices.any { it is ArchiveItem.Card.PlaceHolder }

                when {
                    newItemsEmpty || isDiffFilter && hasPlaceHolder -> 500L
                    else -> 0L
                }
            }
            .map { (_, newItems) ->
                mutation {
                    copy(
                        isLoading = false,
                        listState = listState ?: savedListState.initialListState(),
                        items = preserveKeys(newItems = newItems).itemsWithHeaders,
                    )
                }
            },
        archivesAvailable.map { count ->
            mutation {
                copy(queryState = queryState.copy(count = count))
            }
        },
        queries.map { query ->
            mutation {
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
        }
    )
}

/**
 * Make sure [ArchiveQuery.offset] is in multiples of [ArchiveQuery.limit] and that load more queries match the
 * current query
 */
private fun ArchiveQuery?.stabilizeQuery(
    queriedFetch: Action.Fetch.QueriedFetch,
): ArchiveQuery? {
    val query = when (this) {
        null -> queriedFetch.query
        else -> when (queriedFetch) {
            is Action.Fetch.QueryChange -> queriedFetch.query
            is Action.Fetch.LoadAround -> when {
                queriedFetch.query.hasTheSameFilter(this) -> queriedFetch.query
                else -> return null
            }
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
