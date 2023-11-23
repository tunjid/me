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

package com.tunjid.me.archivedetail


import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.feature.FeatureWhileSubscribed
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.di.downcast
import com.tunjid.me.scaffold.di.restoreState
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.WindowSizeClass
import com.tunjid.me.scaffold.globalui.adaptive.thumbnailSharedElementKey
import com.tunjid.me.scaffold.globalui.navBarSize
import com.tunjid.me.scaffold.globalui.navBarSizeMutations
import com.tunjid.me.scaffold.isInPrimaryNavMutations
import com.tunjid.me.scaffold.nav.NavMutation
import com.tunjid.me.scaffold.nav.consumeNavActions
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.MultiStackNav
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

typealias ArchiveDetailStateHolder = ActionStateProducer<Action, StateFlow<State>>

@Inject
class ArchiveDetailStateHolderCreator(
    creator: (scope: CoroutineScope, savedState: ByteArray?, route: ArchiveDetailRoute) -> ArchiveDetailStateHolder,
) : ScreenStateHolderCreator by creator.downcast()

@Inject
class ActualArchiveDetailStateHolder(
    archiveRepository: ArchiveRepository,
    authRepository: AuthRepository,
    byteSerializer: ByteSerializer,
    uiStateFlow: StateFlow<UiState>,
    navStateFlow: StateFlow<MultiStackNav>,
    navActions: (NavMutation) -> Unit,
    scope: CoroutineScope,
    savedState: ByteArray?,
    route: ArchiveDetailRoute,
) : ArchiveDetailStateHolder by scope.actionStateFlowProducer(
    initialState = byteSerializer.restoreState(savedState) ?: State(
        kind = route.kind,
        sharedElementKey = thumbnailSharedElementKey(route.archiveThumbnail),
        navBarSize = uiStateFlow.value.navBarSize,
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    mutationFlows = listOf(
        uiStateFlow.navBarSizeMutations { copy(navBarSize = it) },
        uiStateFlow.paneMutations(),
        authRepository.authMutations(),
        archiveRepository.archiveLoadMutations(
            id = route.archiveId
        ),
        navStateFlow.isInPrimaryNavMutations(
            route = route,
            mutation = { copy(isInPrimaryNav = it) }
        ),
    ),
    actionTransform = { actions ->
        actions.toMutationStream {
            when (val type = type()) {
                is Action.Navigate -> type.flow.consumeNavActions(
                    mutationMapper = Action.Navigate::navMutation,
                    action = navActions
                )
            }
        }
    }
)

private fun StateFlow<UiState>.paneMutations(): Flow<Mutation<State>> =
    map { (it.windowSizeClass > WindowSizeClass.COMPACT) to it.paneAnchor }
        .distinctUntilChanged()
        .mapToMutation { (hasSecondaryPanel, paneSplit) ->
            copy(
                hasSecondaryPanel = hasSecondaryPanel,
                paneAnchor = paneSplit,
            )
        }

private fun AuthRepository.authMutations(): Flow<Mutation<State>> =
    signedInUserStream
        .mapToMutation {
            copy(
                signedInUserId = it?.id,
                hasFetchedAuthStatus = true,
            )
        }

private fun ArchiveRepository.archiveLoadMutations(
    id: ArchiveId?,
): Flow<Mutation<State>> =
    if (id == null) emptyFlow()
    else archiveStream(id = id)
        .mapToMutation { fetchedArchive ->
            copy(
                sharedElementKey = thumbnailSharedElementKey(fetchedArchive?.thumbnail),
                wasDeleted = archive != null && fetchedArchive == null,
                archive = fetchedArchive
            )
        }
