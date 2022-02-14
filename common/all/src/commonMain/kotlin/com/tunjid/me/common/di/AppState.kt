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

package com.tunjid.me.common.di

import com.tunjid.me.globalui.GlobalUiMutator
import com.tunjid.me.globalui.UiState
import com.tunjid.me.nav.NavMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.asNoOpStateFlowMutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.MultiStackNav
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

data class AppState(
    val nav: MultiStackNav,
    val ui: UiState,
    val isInForeground: Boolean = true,
    val routeIdsToSerializedStates: Map<String, ByteArray> = mapOf()
)

sealed class AppAction {
    data class AppStatus(
        val isInForeground: Boolean
    ) : AppAction()

    data class RestoreSerializedStates(
        val routeIdsToSerializedStates: Map<String, ByteArray>
    ) : AppAction()
}

private typealias BackingAppMutator = Mutator<AppAction, StateFlow<AppState>>

interface AppMutator : BackingAppMutator {
    val navMutator: NavMutator
    val globalUiMutator: GlobalUiMutator
}

val AppState.asAppMutator: AppMutator
    get() {
        val appState = this
        val baseMutator: Mutator<AppAction, StateFlow<AppState>> = appState.asNoOpStateFlowMutator()
        return object : AppMutator, Mutator<AppAction, StateFlow<AppState>> by baseMutator {
            override val navMutator: NavMutator = appState.nav.asNoOpStateFlowMutator()
            override val globalUiMutator: GlobalUiMutator = appState.ui.asNoOpStateFlowMutator()
        }
    }

fun <T> Flow<T>.monitorWhenActive(mutator: AppMutator) =
    mutator.state
        .map { it.isInForeground }
        .distinctUntilChanged()
        .flatMapLatest { isInForeground ->
            if (isInForeground) this
            else emptyFlow()
        }

fun appMutator(
    scope: CoroutineScope,
    globalUiMutator: GlobalUiMutator,
    navMutator: NavMutator
): AppMutator = object : AppMutator, BackingAppMutator by backingAppMutator(
    scope = scope,
    globalUiMutator = globalUiMutator,
    navMutator = navMutator
) {
    override val navMutator = navMutator

    override val globalUiMutator = globalUiMutator
}

private fun backingAppMutator(
    scope: CoroutineScope,
    globalUiMutator: GlobalUiMutator,
    navMutator: NavMutator
): BackingAppMutator = stateFlowMutator(
    scope = scope,
    started = SharingStarted.Eagerly,
    initialState = AppState(
        nav = navMutator.state.value,
        ui = globalUiMutator.state.value,
    ),
    actionTransform = { actions ->
        merge(
            navMutator.state.map {
                Mutation { copy(nav = it) }
            },
            globalUiMutator.state.map {
                Mutation { copy(ui = it) }
            },
            actions.toMutationStream {
                when (val action = type()) {
                    is AppAction.AppStatus -> action.flow.foregroundMutations()
                    is AppAction.RestoreSerializedStates -> action.flow.stateRestorationMutations()
                }
            }
        )
    }
)

private fun Flow<AppAction.AppStatus>.foregroundMutations(): Flow<Mutation<AppState>> =
    distinctUntilChanged()
        .map {
            Mutation { copy(isInForeground = it.isInForeground) }
        }

private fun Flow<AppAction.RestoreSerializedStates>.stateRestorationMutations(): Flow<Mutation<AppState>> =
    distinctUntilChanged()
        .map {
            Mutation { copy(routeIdsToSerializedStates = it.routeIdsToSerializedStates) }
        }
