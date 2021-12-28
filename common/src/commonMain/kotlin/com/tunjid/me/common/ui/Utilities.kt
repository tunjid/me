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

package com.tunjid.me.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.me.common.LocalAppDependencies
import com.tunjid.me.common.globalui.UiState
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Instant
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun <T, R> StateFlow<T>.mapState(scope: CoroutineScope, mapper: (T) -> R) =
    map { mapper(it) }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            initialValue = mapper(value),
            started = SharingStarted.WhileSubscribed(2000),
        )

private data class MutableFunction<T>(var backing: (T) -> Unit = {}) : (T) -> Unit {
    override fun invoke(item: T) = backing(item)
}

@Composable
fun <T, R> StateFlow<T>.mappedCollectAsState(
    context: CoroutineContext = EmptyCoroutineContext,
    mapper: (T) -> R
): State<R> {
    val scope = rememberCoroutineScope()
    return mapState(scope = scope, mapper = mapper).collectAsState(context = context)
}

@Composable
fun InitialUiState(state: UiState) {
    val uiStateHolder = LocalAppDependencies.current.globalUiMutator

    val toolbarMenuClickListener = remember {
        MutableFunction(state.toolbarMenuClickListener)
    }
    val altToolbarMenuClickListener = remember {
        MutableFunction(state.altToolbarMenuClickListener)
    }

    uiStateHolder.accept(Mutation {
        state.copy(
            systemUI = systemUI,
            toolbarMenuClickListener = toolbarMenuClickListener,
            altToolbarMenuClickListener = altToolbarMenuClickListener
        )
    })

    DisposableEffect(true) {
        onDispose {
            toolbarMenuClickListener.backing = {}
            altToolbarMenuClickListener.backing = {}
        }
    }
}

data class UISizes(
    val toolbarSize: Dp,
    val bottomNavSize: Dp,
    val snackbarPadding: Dp,
    val navBarHeightThreshold: Dp
)

val uiSizes = UISizes(
    toolbarSize = 56.dp,
    bottomNavSize = 56.dp,
    snackbarPadding = 8.dp,
    navBarHeightThreshold = 80.dp
)

infix fun Dp.countIf(condition: Boolean) = if (condition) this else 0.dp

infix fun Int.countIf(condition: Boolean) = if (condition) this else 0

fun <T : Any, R : Any> T.asNoOpStateFlowMutator() = object : Mutator<R, StateFlow<T>> {
    override val accept: (R) -> Unit = {}
    override val state: StateFlow<T> = MutableStateFlow(this@asNoOpStateFlowMutator)
}
