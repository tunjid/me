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

package com.tunjid.me.common.ui.signin


import com.tunjid.me.common.app.AppMutator
import com.tunjid.me.common.app.monitorWhenActive
import com.tunjid.me.common.data.repository.AuthRepository
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import com.tunjid.mutator.coroutines.toMutationStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

typealias SignInMutator = Mutator<Action, StateFlow<State>>

fun signInMutator(
    scope: CoroutineScope,
    route: SignInRoute,
    initialState: State? = null,
    authRepository: AuthRepository,
    appMutator: AppMutator,
): SignInMutator = stateFlowMutator(
    scope = scope,
    initialState = initialState ?: State(),
    started = SharingStarted.WhileSubscribed(2000),
    actionTransform = { actions ->
        merge(
            authRepository.isSignedIn.map { Mutation { copy(isSignedIn = it) } },
            actions.toMutationStream {
                when (val action = type()) {
                    is Action.FieldChanged -> action.flow.formEditMutations()
                    is Action.Submit -> action.flow.submissionMutations(authRepository)
                }
            }
        ).monitorWhenActive(appMutator)
    }
)

private fun Flow<Action.FieldChanged>.formEditMutations(): Flow<Mutation<State>> =
    map { fieldChangedAction ->
        Mutation {
            copy(
                fields = fields.map { field ->
                    if (field.id == fieldChangedAction.field.id) fieldChangedAction.field
                    else field
                }
            )
        }
    }

private fun Flow<Action.Submit>.submissionMutations(
    authRepository: AuthRepository
): Flow<Mutation<State>> =
    debounce(200)
        .flatMapLatest { (request) ->
            flow <Mutation<State>>{
                emit(Mutation { copy(isSubmitting = true) })
                // TODO: Show snack bar if error
                authRepository.createSession(request = request)
                emit(Mutation { copy(isSubmitting = false) })
            }
        }