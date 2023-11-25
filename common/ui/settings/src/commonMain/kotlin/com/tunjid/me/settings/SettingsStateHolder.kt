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


import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.feature.FeatureWhileSubscribed
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.di.downcast
import com.tunjid.me.scaffold.di.restoreState
import com.tunjid.me.scaffold.navigation.NavigationMutation
import com.tunjid.me.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject

typealias SettingsStateHolder = ActionStateProducer<Action, StateFlow<State>>

@Inject
class SettingsStateHolderCreator(
    creator: (scope: CoroutineScope, savedState: ByteArray?, route: SettingsRoute) -> SettingsStateHolder
) : ScreenStateHolderCreator by creator.downcast()

@Inject
class ActualSettingsStateHolder(
    authRepository: AuthRepository,
    byteSerializer: ByteSerializer,
    navActions: (NavigationMutation) -> Unit,
    scope: CoroutineScope,
    savedState: ByteArray?,
    @Suppress("UNUSED_PARAMETER")
    route: SettingsRoute,
) : SettingsStateHolder by scope.actionStateFlowProducer(
    initialState = byteSerializer.restoreState(savedState) ?: State(),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    mutationFlows = listOf(
        authRepository.isSignedIn.mapToMutation { isSignedIn ->
            copy(
                routes = listOfNotNull(
                    "profile".takeIf { isSignedIn },
                    "sign-in".takeIf { !isSignedIn }
                )
            )
        }
    ),
    actionTransform = { actions ->
        actions.toMutationStream {
            when (val type = type()) {
                is Action.Navigate -> type.flow.consumeNavigationActions(
                    navigationMutationConsumer = navActions
                )
            }
        }
    }
)
