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

package com.tunjid.me.feature.archivefilesparent

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.me.feature.FeatureWhileSubscribed
import com.tunjid.me.feature.archivefiles.ArchiveFilesStateHolderCreator
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.navigation.NavigationMutation
import com.tunjid.me.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias ArchiveFilesParentStateHolder = ActionStateMutator<Action, StateFlow<State>>

@Stable
@Inject
class ArchiveFilesParentStateHolderCreator(
    private val creator: (scope: CoroutineScope, route: Route) -> ActualArchiveFilesParentStateHolder
) : ScreenStateHolderCreator {
    override fun invoke(
        scope: CoroutineScope,
        route: Route
    ): ActualArchiveFilesParentStateHolder = creator.invoke(scope, route)
}

/**
 * Manages [State] for [ArchiveFilesParentRoute]
 */
@Inject
class ActualArchiveFilesParentStateHolder(
    childCreator: ArchiveFilesStateHolderCreator,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    ArchiveFilesParentStateHolder by scope.actionStateFlowMutator(
        initialState = State(
            children = route.children.filterIsInstance<Route>(),
            childCreator = childCreator,
        ),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        actionTransform = stateHolder@{ actions ->
            actions.toMutationStream(keySelector = Action::key) {
                when (val action = type()) {
                    is Action.Navigate -> action.flow.consumeNavigationActions(
                        navigationMutationConsumer = navActions
                    )
                }
            }
        }
    )
