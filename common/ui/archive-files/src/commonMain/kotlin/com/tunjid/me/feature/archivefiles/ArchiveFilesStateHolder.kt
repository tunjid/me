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

package com.tunjid.me.feature.archivefiles

import com.tunjid.me.core.model.ArchiveFileQuery
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.data.repository.ArchiveFileRepository
import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.feature.FeatureWhileSubscribed
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.di.downcast
import com.tunjid.me.scaffold.di.restoreState
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.navRailVisible
import com.tunjid.me.scaffold.nav.NavMutation
import com.tunjid.me.scaffold.nav.NavState
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.mutator.mutation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import me.tatarka.inject.annotations.Inject

typealias ArchiveFilesStateHolder = ActionStateProducer<Action, StateFlow<State>>

@Inject
class ArchiveFilesStateHolderCreator(
    creator: (scope: CoroutineScope, savedState: ByteArray?, route: ArchiveFilesRoute) -> ArchiveFilesStateHolder
) : ScreenStateHolderCreator by creator.downcast()

/**
 * Manages [State] for [ArchiveFilesRoute]
 */
@Inject
class ActualArchiveFilesStateHolder(
    authRepository: AuthRepository,
    archiveFileRepository: ArchiveFileRepository,
    byteSerializer: ByteSerializer,
    navStateFlow: StateFlow<NavState>,
    uiStateFlow: StateFlow<UiState>,
    scope: CoroutineScope,
    savedState: ByteArray?,
    route: ArchiveFilesRoute,
) : ArchiveFilesStateHolder by scope.actionStateFlowProducer(
    initialState = byteSerializer.restoreState(savedState) ?: State(
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    mutationFlows = listOf(
        mainNavContentMutations(
            route = route,
            navStateFlow = navStateFlow,
            uiStateFlow = uiStateFlow
        ),
        authRepository.authMutations(),
        archiveFileRepository.fileMutations(
            archiveId = route.archiveId
        )
    ),
    actionTransform = { actions ->
        actions.toMutationStream(keySelector = Action::key) {
            emptyFlow()
        }
    }
)

private fun AuthRepository.authMutations(): Flow<Mutation<State>> =
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

private fun ArchiveFileRepository.fileMutations(archiveId: ArchiveId): Flow<Mutation<State>> =
    photos(ArchiveFileQuery(archiveId = archiveId))
        .distinctUntilChanged()
        .map {
            mutation {
                copy(
                    files = it,
                )
            }
        }

/**
 * Updates [State] with whether it is the main navigation content
 */
private fun mainNavContentMutations(
    route: ArchiveFilesRoute,
    navStateFlow: StateFlow<NavState>,
    uiStateFlow: StateFlow<UiState>,
) = combine(
    navStateFlow.map { route.id == it.supportingRoute?.id },
    uiStateFlow.map { it.navRailVisible },
    Boolean::and,
)
    .distinctUntilChanged()
    .map {
        mutation<State> { copy(isMainContent = !it) }
    }