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

package com.tunjid.me.common.ui.utilities

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.me.common.app.LocalAppDependencies
import com.tunjid.me.common.globalui.UiState
import com.tunjid.mutator.Mutation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

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
    val uiMutator = LocalAppDependencies.current.appMutator.globalUiMutator

    val fabClickListener = remember {
        MutableFunction(state.fabClickListener)
    }
    val toolbarMenuClickListener = remember {
        MutableFunction(state.toolbarMenuClickListener)
    }
    val altToolbarMenuClickListener = remember {
        MutableFunction(state.altToolbarMenuClickListener)
    }
    val snackbarMessageConsumer = remember {
        MutableFunction(state.snackbarMessageConsumer)
    }

    val immutables = state.copy(
        fabClickListener = fabClickListener,
        toolbarMenuClickListener = toolbarMenuClickListener,
        altToolbarMenuClickListener = altToolbarMenuClickListener,
        snackbarMessageConsumer = snackbarMessageConsumer,
    )

    LaunchedEffect(immutables) {
        uiMutator.accept(Mutation {
            // Preserve things that should not be overwritten
            state.copy(
                navMode = navMode,
                systemUI = systemUI,
                fabClickListener = fabClickListener,
                toolbarMenuClickListener = toolbarMenuClickListener,
                altToolbarMenuClickListener = altToolbarMenuClickListener,
                snackbarMessageConsumer = snackbarMessageConsumer,
            )
        })
    }

    DisposableEffect(true) {
        onDispose {
            fabClickListener.backing = {}
            toolbarMenuClickListener.backing = {}
            altToolbarMenuClickListener.backing = {}
        }
    }
}

object UiSizes {
    val toolbarSize = 56.dp
    val navRailWidth = 72.dp
    val navRailContentWidth = 400.dp
    val bottomNavSize = 56.dp
    val snackbarPeek = 56.dp
}

infix fun Dp.countIf(condition: Boolean) = if (condition) this else 0.dp

private fun <T, R> StateFlow<T>.mapState(scope: CoroutineScope, mapper: (T) -> R) =
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