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

package com.tunjid.me.common.ui.settings


import com.tunjid.me.common.di.AppMutator
import com.tunjid.me.common.di.monitorWhenActive
import com.tunjid.me.common.data.repository.AuthRepository
import com.tunjid.me.common.ui.profile.ProfileRoute
import com.tunjid.me.common.ui.signin.SignInRoute
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

typealias SettingsMutator = Mutator<Action, StateFlow<State>>

fun settingsMutator(
    scope: CoroutineScope,
    route: SettingsRoute,
    initialState: State? = null,
    authRepository: AuthRepository,
    appMutator: AppMutator,
): SettingsMutator = stateFlowMutator(
    scope = scope,
    initialState = initialState ?: State(),
    started = SharingStarted.WhileSubscribed(2000),
    actionTransform = {
        authRepository.isSignedIn.map { isSignedIn ->
            Mutation<State> {
                copy(
                    routes = listOfNotNull(
                        ProfileRoute.takeIf { isSignedIn },
                        SignInRoute.takeIf { !isSignedIn }
                    )
                )
            }
        }
            .monitorWhenActive(appMutator)
    }
)
