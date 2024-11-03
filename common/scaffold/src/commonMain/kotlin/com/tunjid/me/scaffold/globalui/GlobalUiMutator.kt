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
package com.tunjid.me.scaffold.globalui

import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.mutationOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

typealias GlobalUiStateHolder = ActionStateMutator<Mutation<UiState>, StateFlow<UiState>>

@Inject
class ActualGlobalUiStateHolder(
    appScope: CoroutineScope,
) : GlobalUiStateHolder by appScope.actionStateFlowMutator(
    initialState = UiState(),
    actionTransform = { it }
)

fun <State : Any> StateFlow<UiState>.navBarSizeMutations(
    mutation: State.(navbarSize: Int) -> State
): Flow<Mutation<State>> =
    map { it.navBarSize }
        .distinctUntilChanged()
        .map { mutationOf { mutation(this, it) } }