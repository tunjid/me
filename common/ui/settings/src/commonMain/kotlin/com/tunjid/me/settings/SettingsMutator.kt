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

package com.tunjid.me.settings


import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.feature.FeatureWhileSubscribed
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.lifecycle.Lifecycle
import com.tunjid.me.scaffold.lifecycle.monitorWhenActive
import com.tunjid.me.scaffold.nav.NavMutation
import com.tunjid.me.scaffold.nav.consumeNavActions
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.mutator.mutation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

typealias SettingsMutator = ActionStateProducer<Action, StateFlow<State>>

@Inject
class SettingsMutatorCreator(
    creator: (scope: CoroutineScope, route: SettingsRoute) -> SettingsMutator
) : ScreenStateHolderCreator by creator as ScreenStateHolderCreator

@Inject
class ActualSettingsMutator(
    initialState: State? = null,
    authRepository: AuthRepository,
    lifecycleStateFlow: StateFlow<Lifecycle>,
    navActions: (NavMutation) -> Unit,
    scope: CoroutineScope,
    route: SettingsRoute,
) : SettingsMutator by scope.actionStateFlowProducer(
    initialState = initialState ?: State(),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    mutationFlows = listOf(
        authRepository.isSignedIn.map { isSignedIn ->
            mutation<State> {
                copy(
                    routes = listOfNotNull(
                        "profile".takeIf { isSignedIn },
                        "sign-in".takeIf { !isSignedIn }
                    )
                )
            }
        }
            .monitorWhenActive(lifecycleStateFlow)
    ),
    actionTransform = { actions ->
        actions.toMutationStream {
            when (val type = type()) {
                is Action.Navigate -> type.flow.consumeNavActions<Action.Navigate, State>(
                    mutationMapper = Action.Navigate::navMutation,
                    action = navActions
                )
            }
        }.monitorWhenActive(lifecycleStateFlow)
    }
)
