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
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.feature.FeatureWhileSubscribed
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.di.downcast
import com.tunjid.me.scaffold.di.restoreState
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.navRailVisible
import com.tunjid.me.scaffold.nav.NavMutation
import com.tunjid.me.scaffold.nav.NavState
import com.tunjid.me.scaffold.nav.consumeNavActions
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.mutator.mutation
import com.tunjid.tiler.buildTiledList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import me.tatarka.inject.annotations.Inject

typealias ArchiveListStateHolder = ActionStateProducer<Action, StateFlow<State>>

@Inject
class ArchiveListStateHolderCreator(
    creator: (scope: CoroutineScope, savedState: ByteArray?, route: ArchiveListRoute) -> ArchiveListStateHolder
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
    uiStateFlow: StateFlow<UiState>,
    navActions: (NavMutation) -> Unit,
    scope: CoroutineScope,
    savedState: ByteArray?,
    route: ArchiveListRoute,
) : ArchiveListStateHolder by scope.actionStateFlowProducer(
    initialState = byteSerializer.restoreState(savedState) ?: State(
        items = buildTiledList {
            add(
                ArchiveQuery(kind = route.kind),
                ArchiveItem.Loading(
                    isCircular = true,
                )
            )
        },
        queryState = QueryState(
            startQuery = ArchiveQuery(kind = route.kind),
        )
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    mutationFlows = listOf(
        navRailStatusMutations(
            navStateFlow = navStateFlow,
            uiStateFlow = uiStateFlow
        ),
        authRepository.authMutations(),
    ),
    actionTransform = { actions ->
        actions.toMutationStream(keySelector = Action::key) {
            when (val action = type()) {
                is Action.Fetch -> action.flow.fetchMutations(
                    scope = scope,
                    repo = archiveRepository
                )

                is Action.FilterChanged -> action.flow.filterChangedMutations()
                is Action.ToggleFilter -> action.flow.filterToggleMutations()
                is Action.LastVisibleKey -> action.flow.resetScrollMutations()
                is Action.GridSize -> action.flow.gridSizeMutations()
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
 * Updates [State] with whether it is in the nav rail
 */
private fun navRailStatusMutations(
    navStateFlow: StateFlow<NavState>,
    uiStateFlow: StateFlow<UiState>,
) = combine(
    navStateFlow.map {
        val kindMatch = ArchiveKind.values()
            .map(ArchiveKind::type)
            .joinToString(separator = "|")
        val pathMatch = "archive/$kindMatch".toRegex()
        when (val navRailRoute = it.navRail?.id) {
            null -> false
            else -> pathMatch.matches(navRailRoute)
        }
    },
    uiStateFlow.map { it.navRailVisible },
    Boolean::and,
)
    .distinctUntilChanged()
    .map {
        mutation<State> { copy(isInNavRail = it) }
    }

/**
 * Notifies of updates in the archive filter
 */
private fun Flow<Action.FilterChanged>.filterChangedMutations(): Flow<Mutation<State>> =
    map { (descriptor) ->
        mutation {
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
 * Updates [State] with the grid size of the app
 */
private fun Flow<Action.GridSize>.gridSizeMutations(): Flow<Mutation<State>> =
    distinctUntilChanged()
        .map {
            mutation {
                copy(queryState = queryState.copy(gridSize = it.size))
            }
        }

/**
 * Updates [State] with the key of the last item seen so when the route shows up in the nav rail,
 * it is in sync
 */
private fun Flow<Action.LastVisibleKey>.resetScrollMutations(): Flow<Mutation<State>> =
    distinctUntilChanged()
        .map {
            mutation {
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
    .map { fetchResult: FetchResult ->
        mutation {
            val fetchAction = fetchResult.action
            val items = fetchResult.items(default = this.items)
            copy(
                items = items,
                queryState = queryState.copy(
                    startQuery = fetchResult.action.query,
                    expanded = when (fetchAction) {
                        is Action.Fetch.Reset -> true
                        else -> queryState.expanded
                    }
                )
            )
        }
    }
