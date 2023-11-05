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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.tunjid.me.common.ui.theme.AppTheme
import com.tunjid.me.core.ui.dragdrop.rootDragDropModifier
import com.tunjid.me.feature.LocalScreenStateHolderCache
import com.tunjid.me.feature.MeApp
import com.tunjid.me.scaffold.globalui.NavMode
import com.tunjid.me.scaffold.globalui.WindowSizeClass
import com.tunjid.me.scaffold.globalui.scaffold.Scaffold
import com.tunjid.me.scaffold.globalui.toWindowSizeClass
import com.tunjid.me.scaffold.lifecycle.LocalLifecycleStateHolder
import com.tunjid.mutator.mutation
import kotlinx.coroutines.flow.distinctUntilChanged
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener

fun main() {

    application {
        val app: MeApp = remember { createMeApp() }
        val windowState = rememberWindowState()
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Me as a composition"
        ) {
            val density = LocalDensity.current.density
            AppTheme {
                CompositionLocalProvider(
                    LocalScreenStateHolderCache provides app.screenStateHolderCache,
                    LocalLifecycleStateHolder provides app.lifecycleStateHolder,
                ) {
                    Scaffold(
                        modifier = Modifier.rootDragDropModifier(
                            density = density,
                            window = window,
                        ),
                        navStateHolder = app.navStateHolder,
                        globalUiStateHolder = app.globalUiStateHolder,
                    )
                }
            }

            LaunchedEffect(true) {
                window.addWindowFocusListener(object : WindowFocusListener {
                    override fun windowGainedFocus(e: WindowEvent?) {
                        app.lifecycleStateHolder.accept { copy(isInForeground = true) }
                    }

                    override fun windowLostFocus(e: WindowEvent?) {
                        app.lifecycleStateHolder.accept { copy(isInForeground = false) }
                    }
                })
            }

            val currentWidth = windowState.size.width
            LaunchedEffect(currentWidth) {
                snapshotFlow(currentWidth::toWindowSizeClass)
                    .distinctUntilChanged()
                    .collect { windowSizeClass ->
                        app.globalUiStateHolder.accept(mutation {
                            copy(
                                windowSizeClass = windowSizeClass,
                                navMode = when (windowSizeClass) {
                                    WindowSizeClass.COMPACT -> NavMode.BottomNav
                                    else -> NavMode.NavRail
                                }
                            )
                        })
                    }
            }
        }
    }
}
