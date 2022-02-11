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

import com.tunjid.me.common.app.AppMutator
import com.tunjid.me.common.app.consumeWith
import com.tunjid.me.common.app.monitorWhenActive
import com.tunjid.me.common.data.model.Descriptor
import com.tunjid.me.common.data.repository.ArchiveRepository
import com.tunjid.me.common.data.repository.AuthRepository
import com.tunjid.me.common.globalui.navRailVisible
import com.tunjid.me.common.nav.navRailRoute
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import com.tunjid.mutator.coroutines.toMutationStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

typealias ArchiveMutator = Mutator<Action, StateFlow<State>>

/**
 * Manages [State] for [ArchiveRoute]
 */
fun archiveMutator(
    scope: CoroutineScope,
    route: ArchiveRoute,
    initialState: State? = null,
    archiveRepository: ArchiveRepository,
    authRepository: AuthRepository,
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
            authRepository.isSignedIn.map { Mutation { copy(isSignedIn = it) } },
            actions.toMutationStream(keySelector = Action::key) {
                when (val action = type()) {
                    is Action.Fetch -> action.flow.fetchMutations(
                        scope = scope,
                        repo = archiveRepository
                    )
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

/**
 * Updates [State] with whether or not it is in the nav rail
 */
private fun AppMutator.navRailStatusMutations() = state.map { appState ->
    appState.nav.navRailRoute is ArchiveRoute && appState.ui.navRailVisible
}
    .distinctUntilChanged()
    .map {
        Mutation<State> { copy(isInNavRail = it) }
    }

/**
 * Notifies of updates in the archive filter
 */
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

/**
 * Updates [State] with the grid size of the app
 */
private fun Flow<Action.GridSize>.gridSizeMutations(): Flow<Mutation<State>> =
    distinctUntilChanged()
        .map {
            Mutation {
                copy(gridSize = it.size)
            }
        }

/**
 * Updates [State] with the key of the last item seen so when the route shows up in the nav rail,
 * it is in sync
 */
private fun Flow<Action.LastVisibleKey>.resetScrollMutations(): Flow<Mutation<State>> =
    distinctUntilChanged()
        .map {
            Mutation {
                copy(lastVisibleKey = it.itemKey)
            }
        }

/**
 * Converts requests to fetch archives into a list of archives to render
 */
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

private val FetchResult.flattenedArchives: List<ArchiveItem>
    get() = queriedArchives
        .flatten()
        .distinctBy { it.key }

private val FetchResult.hasNoResults: Boolean
    get() = queriedArchives.isEmpty() || queriedArchives.all {
        it.all { items -> items is ArchiveItem.Loading }
    }
