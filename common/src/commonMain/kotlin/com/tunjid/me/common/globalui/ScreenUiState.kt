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

package com.tunjid.me.common.globalui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import com.tunjid.me.common.di.LocalAppDependencies
import com.tunjid.mutator.Mutation

val currentUiState
    @ReadOnlyComposable
    @Composable
    get() = LocalAppDependencies.current.appMutator.globalUiMutator.state.value

/**
 * Provides a way of composing the [UiState] on a global level.
 * This allows for coordination of the UI across navigation destinations.
 */
@Composable
fun ScreenUiState(state: UiState) {
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

/**
 * Generic function that helps override the backing implementation to prevent memory leaks
 */
private data class MutableFunction<T>(var backing: (T) -> Unit = {}) : (T) -> Unit {
    override fun invoke(item: T) = backing(item)
}