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

package com.tunjid.me.archivedetail

import androidx.lifecycle.ViewModel
import com.tunjid.me.archivedetail.di.archiveId
import com.tunjid.me.archivedetail.di.archiveThumbnail
import com.tunjid.me.archivedetail.di.kind
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.feature.FeatureWhileSubscribed
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.isInPrimaryNavMutations
import com.tunjid.me.scaffold.navigation.NavigationMutation
import com.tunjid.me.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias ArchiveDetailStateHolder = ActionStateMutator<Action, StateFlow<State>>

@Inject
class ArchiveDetailStateHolderCreator(
    private val creator: (scope: CoroutineScope, route: Route) -> ActualArchiveDetailStateHolder
) : ScreenStateHolderCreator {
    override fun invoke(
        scope: CoroutineScope,
        route: Route
    ): ActualArchiveDetailStateHolder = creator.invoke(scope, route)
}

@Inject
class ActualArchiveDetailStateHolder(
    archiveRepository: ArchiveRepository,
    authRepository: AuthRepository,
    navStateFlow: StateFlow<MultiStackNav>,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope), ArchiveDetailStateHolder by scope.actionStateFlowMutator(
    initialState = State(
        kind = route.routeParams.kind,
        routeThumbnailUrl = route.routeParams.archiveThumbnail,
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    inputs = listOf(
        authRepository.authMutations(),
        archiveRepository.archiveLoadMutations(
            id = route.routeParams.archiveId
        ),
        navStateFlow.isInPrimaryNavMutations(
            route = route,
            mutation = { copy(isInPrimaryNav = it) }
        ),
    ),
    actionTransform = { actions ->
        actions.toMutationStream {
            when (val type = type()) {
                is Action.Navigate -> type.flow.consumeNavigationActions(
                    navigationMutationConsumer = navActions
                )
            }
        }
    }
)

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
                wasDeleted = archive != null && fetchedArchive == null,
                archive = fetchedArchive
            )
        }
