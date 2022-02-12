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

package com.tunjid.me.common.ui.profile


import com.tunjid.me.common.app.AppMutator
import com.tunjid.me.common.app.monitorWhenActive
import com.tunjid.me.common.data.repository.AuthRepository
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

typealias ProfileMutator = Mutator<Action, StateFlow<State>>

fun profileMutator(
    scope: CoroutineScope,
    route: ProfileRoute,
    initialState: State? = null,
    authRepository: AuthRepository,
    appMutator: AppMutator,
): ProfileMutator = stateFlowMutator(
    scope = scope,
    initialState = initialState ?: State(),
    started = SharingStarted.WhileSubscribed(2000),
    actionTransform = {
        authRepository.isSignedIn.map { Mutation<State> { copy() } }
            .monitorWhenActive(appMutator)
    }
)
