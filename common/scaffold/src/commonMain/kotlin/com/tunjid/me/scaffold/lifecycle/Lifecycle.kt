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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import com.tunjid.me.core.utilities.mapState
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.asNoOpStateFlowMutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.CoroutineContext

typealias LifecycleMutator = ActionStateProducer<Mutation<Lifecycle>, StateFlow<Lifecycle>>

data class Lifecycle(
    val isInForeground: Boolean = true,
)

val LocalLifecycleMutator = staticCompositionLocalOf {
    Lifecycle().asNoOpStateFlowMutator<Mutation<Lifecycle>, Lifecycle>()
}

fun <T> Flow<T>.monitorWhenActive(lifecycleStateFlow: StateFlow<Lifecycle>) =
    lifecycleStateFlow
        .map { it.isInForeground }
        .distinctUntilChanged()
        .flatMapLatest { isInForeground ->
            if (isInForeground) this
            else emptyFlow()
        }

@Composable
fun <T, R> StateFlow<T>.mappedCollectAsStateWithLifecycle(
    context: CoroutineContext = kotlin.coroutines.EmptyCoroutineContext,
    mapper: (T) -> R
): State<R> {
    val lifecycle = LocalLifecycleMutator.current.state
    val scope = rememberCoroutineScope()
    val lifecycleBoundState = remember { mapState(scope = scope, mapper = mapper).monitorWhenActive(lifecycle) }
    return lifecycleBoundState.collectAsState(context = context, initial = mapper(this.value))
}

@Composable
fun <T> StateFlow<T>.collectAsStateWithLifecycle(
    context: CoroutineContext = kotlin.coroutines.EmptyCoroutineContext,
): State<T> {
    val lifecycle = LocalLifecycleMutator.current.state
    val scope = rememberCoroutineScope()
    val lifecycleBoundState = remember { this.monitorWhenActive(lifecycle) }
    return collectAsState(context = context)
}

@Inject
class ActualLifecycleMutator(
    appScope: CoroutineScope,
) : LifecycleMutator by appScope.actionStateFlowProducer(
    started = SharingStarted.Eagerly,
    initialState = Lifecycle(),
    actionTransform = { it }
)
