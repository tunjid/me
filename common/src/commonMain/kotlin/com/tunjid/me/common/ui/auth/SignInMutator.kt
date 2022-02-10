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

package com.tunjid.me.common.ui.auth


import com.tunjid.me.common.app.AppMutator
import com.tunjid.me.common.app.monitorWhenActive
import com.tunjid.me.common.data.ByteSerializable
import com.tunjid.me.common.data.repository.ArchiveRepository
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import com.tunjid.mutator.coroutines.toMutationStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.Serializable


@Serializable
data class FormField(
    val id: String,
    val value: String
)

@Serializable
data class State(
    val fields: List<FormField> = listOf(
        FormField(
            id = "email",
            value = "",
        ),
        FormField(
            id = "password",
            value = "",
        )
    )
) : ByteSerializable

sealed class Action {
    data class FieldChanged(val field: FormField) : Action()
}

typealias SignInMutator = Mutator<Action, StateFlow<State>>

fun signInMutator(
    scope: CoroutineScope,
    route: SignInRoute,
    initialState: State? = null,
    appMutator: AppMutator,
): SignInMutator = stateFlowMutator(
    scope = scope,
    initialState = initialState ?: State(),
    started = SharingStarted.WhileSubscribed(2000),
    actionTransform = { actions ->
        merge<Mutation<State>>(
            emptyFlow(),
            actions.toMutationStream {
                when (val action = type()) {
                    is Action.FieldChanged -> action.flow.map { fieldChangedAction ->
                        Mutation {
                            copy(
                                fields = fields.map { field ->
                                    if (field.id == fieldChangedAction.field.id) fieldChangedAction.field
                                    else field
                                }
                            )
                        }
                    }
                }
            }
        ).monitorWhenActive(appMutator)
    }
)
