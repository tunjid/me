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

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.window.core.layout.WindowSizeClass
import com.tunjid.me.common.di.MeApp
import com.tunjid.me.common.ui.theme.AppTheme
import com.tunjid.me.scaffold.globalui.COMPACT
import com.tunjid.me.scaffold.globalui.NavMode
import com.tunjid.me.scaffold.globalui.toWindowSizeClass
import com.tunjid.me.scaffold.scaffold.MeApp
import kotlinx.coroutines.flow.distinctUntilChanged

fun main() {

    application {
        val app: MeApp = remember { createMeApp() }
        val windowState = rememberWindowState()
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Me as a composition"
        ) {
            AppTheme {
                MeApp(
                    modifier = Modifier,
                    appState = app.appState,
                )
            }

            val currentWidth = windowState.size.width
            LaunchedEffect(currentWidth) {
                snapshotFlow(currentWidth::toWindowSizeClass)
                    .distinctUntilChanged()
                    .collect { windowSizeClass ->
                        app.appState.updateGlobalUi {
                            copy(
                                windowSizeClass = windowSizeClass,
                                navMode = when (windowSizeClass) {
                                    WindowSizeClass.COMPACT -> NavMode.BottomNav
                                    else -> NavMode.NavRail
                                }
                            )
                        }
                    }
            }
        }
    }
}
