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

package com.tunjid.me.globalui

import androidx.compose.runtime.staticCompositionLocalOf
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.asNoOpStateFlowMutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

typealias GlobalUiMutator = Mutator<Mutation<UiState>, StateFlow<UiState>>

fun globalUiMutator(scope: CoroutineScope, initialState: UiState = UiState()): GlobalUiMutator =
    stateFlowMutator(
        scope = scope,
        initialState = initialState,
        actionTransform = { it }
    )

val LocalGlobalUiMutator = staticCompositionLocalOf {
    UiState().asNoOpStateFlowMutator<Mutation<UiState>, UiState>()
}

fun <State : Any> GlobalUiMutator.navBarSizeMutations(
    mutation: State.(navbarSize: Int) -> State
): Flow<Mutation<State>> = state
    .map { it.navBarSize }
    .distinctUntilChanged()
    .map { Mutation { mutation(this, it) } }