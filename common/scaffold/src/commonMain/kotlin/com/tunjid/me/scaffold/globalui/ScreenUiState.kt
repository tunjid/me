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

package com.tunjid.me.scaffold.globalui

import androidx.compose.runtime.*
import com.tunjid.mutator.mutation

val currentUiState
    @ReadOnlyComposable
    @Composable
    get() = LocalGlobalUiStateHolder.current.state.value

/**
 * Provides a way of composing the [UiState] on a global level.
 * This allows for coordination of the UI across navigation destinations.
 */
@Composable
fun ScreenUiState(state: UiState) {
    val uiStateHolder = LocalGlobalUiStateHolder.current
    var immutables by remember { mutableStateOf(state) }

    val fabClickListener = MutableFunction(state.fabClickListener)
    val toolbarMenuClickListener = MutableFunction(state.toolbarMenuClickListener)
    val altToolbarMenuClickListener = MutableFunction(state.altToolbarMenuClickListener)
    val snackbarMessageConsumer = MutableFunction(state.snackbarMessageConsumer)

    immutables = state.copy(
        fabClickListener = fabClickListener,
        toolbarMenuClickListener = toolbarMenuClickListener,
        altToolbarMenuClickListener = altToolbarMenuClickListener,
        snackbarMessageConsumer = snackbarMessageConsumer,
    )

    LaunchedEffect(immutables) {
        uiStateHolder.accept(mutation {
            // Preserve things that should not be overwritten
            immutables.copy(
                navMode = navMode,
                systemUI = systemUI,
            )
        })
    }

    DisposableEffect(true) {
        onDispose {
            fabClickListener.backing = {}
            toolbarMenuClickListener.backing = {}
            altToolbarMenuClickListener.backing = {}
            snackbarMessageConsumer.backing = {}
        }
    }
}

/**
 * Syntactic sugar for [remember] remembering a single argument function
 */
@Composable
fun <T> rememberFunction(
    vararg keys: Any?,
    implementation: (T) -> Unit
): (T) -> Unit = remember(*keys) { implementation }

/**
 * Generic function that helps override the backing implementation to prevent memory leaks
 */
private data class MutableFunction<T>(var backing: (T) -> Unit = {}) : (T) -> Unit {
    override fun invoke(item: T) = backing(item)
}