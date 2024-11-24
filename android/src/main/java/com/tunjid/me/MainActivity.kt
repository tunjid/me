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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.lifecycleScope
import androidx.window.core.layout.WindowSizeClass
import androidx.window.layout.WindowMetricsCalculator
import com.tunjid.me.common.ui.theme.AppTheme
import com.tunjid.me.scaffold.globalui.COMPACT
import com.tunjid.me.scaffold.globalui.EXPANDED
import com.tunjid.me.scaffold.globalui.MEDIUM
import com.tunjid.me.scaffold.globalui.NavMode
import com.tunjid.me.scaffold.globalui.PredictiveBackEffects
import com.tunjid.me.scaffold.globalui.insetMutations
import com.tunjid.me.scaffold.globalui.toWindowSizeClass
import com.tunjid.me.scaffold.scaffold.MeApp
import com.tunjid.me.scaffold.scaffold.AppState
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = applicationContext as App
        val meApp = app.meApp
        val appState = meApp.appState

        val root = ComposeView(context = this)

        root.setContent {
            AppTheme {
                MeApp(
//                    modifier = Modifier.rootDragDropModifier(
//                        dragTriggers = setOf(
//                            DragTrigger.LongPress,
//                            DragTrigger.DoubleTap
//                        ),
//                        view = root
//                    ),
                    modifier = Modifier,
                    appState = meApp.appState,
                )
                AdaptNavigation(
                    appState = appState
                )
                PredictiveBackEffects(
                    appState = appState
                )
                LaunchedEffect(appState.globalUi.statusBarColor) {
                    window.statusBarColor = appState.globalUi.statusBarColor
                }
                LaunchedEffect(appState.globalUi.navBarColor) {
                    window.navigationBarColor = appState.globalUi.navBarColor
                }
            }
        }

        setContentView(root)

        lifecycleScope.launch {
            insetMutations().collect(appState::updateGlobalUi)
        }
    }
}

@Composable
private fun MainActivity.AdaptNavigation(appState: AppState) {
    val configuration = LocalConfiguration.current
    val windowMetrics = remember(configuration) {
        WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(this)
    }
    val windowDpSize = with(LocalDensity.current) {
        windowMetrics.bounds.toComposeRect().size.toDpSize()
    }
    val widthWindowSizeClass = windowDpSize.width.toWindowSizeClass()

//    val heightWindowSizeClass = when {
//        windowDpSize.height < 480.dp -> WindowSizeClass.COMPACT
//        windowDpSize.height < 900.dp -> WindowSizeClass.MEDIUM
//        else -> WindowSizeClass.EXPANDED
//    }

    LaunchedEffect(widthWindowSizeClass) {
        appState.updateGlobalUi {
            copy(
                windowSizeClass = widthWindowSizeClass,
                navMode = when (widthWindowSizeClass) {
                    WindowSizeClass.COMPACT -> NavMode.BottomNav
                    WindowSizeClass.MEDIUM -> NavMode.NavRail
                    WindowSizeClass.EXPANDED -> NavMode.NavRail
                    else -> NavMode.NavRail
                }
            )
        }
    }
}
