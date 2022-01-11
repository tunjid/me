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

package com.tunjid.me

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.tunjid.me.common.createAppDependencies
import com.tunjid.me.common.data.AppDatabase
import com.tunjid.me.common.data.DatabaseDriverFactory
import com.tunjid.me.common.globalui.NavMode
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.ui.scaffold.Root
import com.tunjid.me.common.ui.theme.AppTheme
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.accept
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged

fun main() {
    application {
        val appDependencies = createAppDependencies(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
            initialUiState = UiState(navMode = NavMode.NavRail),
            database = AppDatabase.invoke(DatabaseDriverFactory().createDriver())
        )
        val globalUiMutator: Mutator<Mutation<UiState>, StateFlow<UiState>> =
            appDependencies.appMutator.globalUiMutator

        val windowState = rememberWindowState()
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Me as a composition"
        ) {
            AppTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Root(dependencies = appDependencies)
                }
            }

            val currentWidth = windowState.size.width
            LaunchedEffect(currentWidth) {
                snapshotFlow { currentWidth < 600.dp }
                    .distinctUntilChanged()
                    .collect { isInPortrait ->
                        globalUiMutator.accept {
                            copy(navMode = if (isInPortrait) NavMode.BottomNav else NavMode.NavRail)
                        }
                    }
            }
        }
    }
}


