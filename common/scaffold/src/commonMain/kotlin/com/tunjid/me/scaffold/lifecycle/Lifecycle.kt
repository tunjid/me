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

import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import me.tatarka.inject.annotations.Inject

typealias LifecycleMutator = ActionStateProducer<Mutation<Lifecycle>, StateFlow<Lifecycle>>

data class Lifecycle(
    val isInForeground: Boolean = true,
)

fun <T> Flow<T>.monitorWhenActive(lifecycleStateFlow: StateFlow<Lifecycle>) =
    lifecycleStateFlow
        .map { it.isInForeground }
        .distinctUntilChanged()
        .flatMapLatest { isInForeground ->
            if (isInForeground) this
            else emptyFlow()
        }

fun <T> List<Flow<T>>.monitorWhenActive(lifecycleStateFlow: StateFlow<Lifecycle>) =
    map {
        lifecycleStateFlow
            .map { it.isInForeground }
            .distinctUntilChanged()
            .flatMapLatest { isInForeground ->
                if (isInForeground) it
                else emptyFlow()
            }
    }

@Inject
class ActualLifecycleMutator(
    appScope: CoroutineScope,
) : LifecycleMutator by appScope.actionStateFlowProducer(
    started = SharingStarted.Eagerly,
    initialState = Lifecycle(),
    actionTransform = { it }
)
