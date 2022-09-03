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
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.feature.FeatureWhileSubscribed
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.navBarSize
import com.tunjid.me.scaffold.globalui.navBarSizeMutations
import com.tunjid.me.scaffold.lifecycle.Lifecycle
import com.tunjid.me.scaffold.lifecycle.monitorWhenActive
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.mutation
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

typealias ArchiveDetailMutator = ActionStateProducer<Unit, StateFlow<State>>

fun archiveDetailMutator(
    scope: CoroutineScope,
    route: ArchiveDetailRoute,
    initialState: State? = null,
    archiveRepository: ArchiveRepository,
    authRepository: AuthRepository,
    uiStateFlow: StateFlow<UiState>,
    lifecycleStateFlow: StateFlow<Lifecycle>,
): ArchiveDetailMutator = scope.actionStateFlowProducer(
    initialState = initialState ?: State(
        kind = route.kind,
        navBarSize = uiStateFlow.value.navBarSize,
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    actionTransform = {
        merge(
            uiStateFlow.navBarSizeMutations { copy(navBarSize = it) },
            authRepository.authMutations(),
            archiveRepository.archiveLoadMutations(
                kind = route.kind,
                id = route.archiveId
            )
        ).monitorWhenActive(lifecycleStateFlow)
    }
)

private fun AuthRepository.authMutations(): Flow<Mutation<State>> =
    signedInUserStream.map {
        mutation {
            copy(
                signedInUserId = it?.id,
                hasFetchedAuthStatus = true,
            )
        }
    }

private fun ArchiveRepository.archiveLoadMutations(
    id: ArchiveId,
    kind: ArchiveKind
): Flow<Mutation<State>> = monitorArchive(
    kind = kind,
    id = id
)
    .map { fetchedArchive ->
        mutation {
            copy(
                wasDeleted = archive != null && fetchedArchive == null,
                archive = fetchedArchive
            )
        }
    }