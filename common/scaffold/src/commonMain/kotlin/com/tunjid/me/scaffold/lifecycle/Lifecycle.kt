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

import androidx.compose.runtime.*
import com.tunjid.me.core.utilities.mapState
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.asNoOpStateFlowMutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.CoroutineContext

typealias LifecycleStateHolder = ActionStateProducer<Mutation<Lifecycle>, StateFlow<Lifecycle>>

data class Lifecycle(
    val isInForeground: Boolean = true,
)

val LocalLifecycleStateHolder = staticCompositionLocalOf {
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
    val lifecycle = LocalLifecycleStateHolder.current.state
    val scope = rememberCoroutineScope()
    val lifecycleBoundState = remember { mapState(scope = scope, mapper = mapper).monitorWhenActive(lifecycle) }
    val initial = remember { mapper(value) }

    return lifecycleBoundState.collectAsState(
        context = context,
        initial = initial
    )
}

@Composable
fun <T> StateFlow<T>.collectAsStateWithLifecycle(
    context: CoroutineContext = kotlin.coroutines.EmptyCoroutineContext,
): State<T> {
    val lifecycle = LocalLifecycleStateHolder.current.state
    val lifecycleBoundState = remember { monitorWhenActive(lifecycle) }
    val initial = remember { value }
    return lifecycleBoundState.collectAsState(
        context = context,
        initial = initial
    )
}

@Inject
class ActualLifecycleStateHolder(
    appScope: CoroutineScope,
) : LifecycleStateHolder by appScope.actionStateFlowProducer(
    started = SharingStarted.Eagerly,
    initialState = Lifecycle(),
    actionTransform = { it }
)

/**
 * [State] that can be mutated by [Action]
 */
data class ActionableState<Action : Any, State : Any>(
    val state: State,
    val actions: (Action) -> Unit
)

@Composable
fun <Action : Any, State : Any> ActionStateProducer<Action, StateFlow<State>>.toActionableState(

): androidx.compose.runtime.State<ActionableState<Action, State>> {
    val stateFlow = remember {
        state
    }
    val actions = remember {
        accept
    }
    val state by stateFlow.collectAsStateWithLifecycle()
    val actionableState = remember(state) {
        derivedStateOf {
            ActionableState(
                state = state,
                actions = actions
            )
        }
    }
    return actionableState
}