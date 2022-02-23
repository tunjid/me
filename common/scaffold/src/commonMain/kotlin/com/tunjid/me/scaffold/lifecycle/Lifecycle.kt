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

package com.tunjid.me.scaffold.lifecycle

import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import com.tunjid.mutator.coroutines.toMutationStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

typealias LifecycleMutator = Mutator<LifecycleAction, StateFlow<Lifecycle>>

data class Lifecycle(
    val isInForeground: Boolean = true,
    val routeIdsToSerializedStates: Map<String, ByteArray> = mapOf()
)

sealed class LifecycleAction {
    data class LifecycleStatus(
        val isInForeground: Boolean
    ) : LifecycleAction()

    data class RestoreSerializedStates(
        val routeIdsToSerializedStates: Map<String, ByteArray>
    ) : LifecycleAction()
}

fun <T> Flow<T>.monitorWhenActive(lifecycleStateFlow: StateFlow<Lifecycle>) =
    lifecycleStateFlow
        .map { it.isInForeground }
        .distinctUntilChanged()
        .flatMapLatest { isInForeground ->
            if (isInForeground) this
            else emptyFlow()
        }

internal fun lifecycleMutator(
    scope: CoroutineScope,
): LifecycleMutator = stateFlowMutator(
    scope = scope,
    started = SharingStarted.Eagerly,
    initialState = Lifecycle(),
    actionTransform = { actions ->
        actions.toMutationStream {
            when (val action = type()) {
                is LifecycleAction.LifecycleStatus -> action.flow.foregroundMutations()
                is LifecycleAction.RestoreSerializedStates -> action.flow.stateRestorationMutations()
            }
        }
    }
)

private fun Flow<LifecycleAction.LifecycleStatus>.foregroundMutations(): Flow<Mutation<Lifecycle>> =
    distinctUntilChanged()
        .map {
            Mutation { copy(isInForeground = it.isInForeground) }
        }

private fun Flow<LifecycleAction.RestoreSerializedStates>.stateRestorationMutations(): Flow<Mutation<Lifecycle>> =
    distinctUntilChanged()
        .map {
            Mutation { copy(routeIdsToSerializedStates = it.routeIdsToSerializedStates) }
        }
