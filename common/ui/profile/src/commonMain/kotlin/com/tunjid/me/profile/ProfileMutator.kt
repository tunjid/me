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

package com.tunjid.me.profile


import com.tunjid.me.core.ui.update
import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.feature.FeatureWhileSubscribed
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.lifecycle.Lifecycle
import com.tunjid.me.scaffold.lifecycle.monitorWhenActive
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.mutator.mutation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import me.tatarka.inject.annotations.Inject

typealias ProfileMutator = ActionStateProducer<Action, StateFlow<State>>

@Inject
class ProfileMutatorCreator(
    creator: (scope: CoroutineScope, route: ProfileRoute) -> ProfileMutator
) : ScreenStateHolderCreator by creator as ScreenStateHolderCreator

@Inject
class ActualProfileMutator(
    initialState: State? = null,
    authRepository: AuthRepository,
    lifecycleStateFlow: StateFlow<Lifecycle>,
    scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER")
    route: ProfileRoute,
) : ProfileMutator by scope.actionStateFlowProducer(
    initialState = initialState ?: State(),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    actionTransform = { actions ->
        merge(
            authRepository.signedInUserStream.map { mutation { copy(signedInUser = it) } },
            actions.toMutationStream {
                when (val action = type()) {
                    is Action.FieldChanged -> action.flow.formEditMutations()
                }
            }
        )
            .monitorWhenActive(lifecycleStateFlow)
    }
)

private fun Flow<Action.FieldChanged>.formEditMutations(): Flow<Mutation<State>> =
    map { (updatedField) ->
        mutation {
            copy(fields = fields.update(updatedField))
        }
    }