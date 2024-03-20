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
package com.tunjid.me.profile


import com.tunjid.me.core.ui.update
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.feature.FeatureWhileSubscribed
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.di.downcast
import com.tunjid.me.scaffold.di.restoreState
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.mutator.mutationOf
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import me.tatarka.inject.annotations.Inject

typealias ProfileStateHolder = ActionStateMutator<Action, StateFlow<State>>

@Inject
class ProfileStateHolderCreator(
    creator: (scope: CoroutineScope, savedState: ByteArray?, route: Route) -> ProfileStateHolder
) : ScreenStateHolderCreator by creator.downcast()

@Inject
class ActualProfileStateHolder(
    authRepository: AuthRepository,
    byteSerializer: ByteSerializer,
    scope: CoroutineScope,
    savedState: ByteArray?,
    @Suppress("UNUSED_PARAMETER")
    route: Route,
) : ProfileStateHolder by scope.actionStateFlowMutator(
    initialState = byteSerializer.restoreState(savedState) ?: State(),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    inputs = listOf(
        authRepository.signedInUserStream.map { mutationOf { copy(signedInUser = it) } },
    ),
    actionTransform = { actions ->
        actions.toMutationStream {
            when (val action = type()) {
                is Action.FieldChanged -> action.flow.formEditMutations()
            }
        }
    }
)

private fun Flow<Action.FieldChanged>.formEditMutations(): Flow<Mutation<State>> =
    map { (updatedField) ->
        mutationOf {
            copy(fields = fields.update(updatedField))
        }
    }