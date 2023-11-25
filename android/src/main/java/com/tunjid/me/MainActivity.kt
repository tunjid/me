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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.WindowMetricsCalculator
import com.tunjid.me.common.ui.theme.AppTheme
import com.tunjid.me.core.ui.dragdrop.DragTrigger
import com.tunjid.me.core.ui.dragdrop.rootDragDropModifier
import com.tunjid.me.feature.LocalScreenStateHolderCache
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.NavMode
import com.tunjid.me.scaffold.globalui.WindowSizeClass
import com.tunjid.me.scaffold.globalui.insetMutations
import com.tunjid.me.scaffold.globalui.integrateBackActions
import com.tunjid.me.scaffold.scaffold.Scaffold
import com.tunjid.me.scaffold.globalui.toWindowSizeClass
import com.tunjid.me.scaffold.lifecycle.LocalLifecycleStateHolder
import com.tunjid.mutator.mutation
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = applicationContext as App
        val meApp = app.meApp

        integrateBackActions(
            globalUiStateHolder = meApp.globalUiStateHolder,
            navStateHolder = meApp.navStateHolder,
        )

        val root = ComposeView(context = this)

        root.setContent {
            AppTheme {
                CompositionLocalProvider(
                    LocalScreenStateHolderCache provides meApp.screenStateHolderCache,
                    LocalLifecycleStateHolder provides meApp.lifecycleStateHolder,
                ) {
                    Scaffold(
                        modifier = Modifier.rootDragDropModifier(
                            dragTriggers = setOf(
                                DragTrigger.LongPress,
                                DragTrigger.DoubleTap
                            ),
                            view = root
                        ),
                        routeParser = meApp.routeParser,
                        navStateHolder = meApp.navStateHolder,
                        globalUiStateHolder = meApp.globalUiStateHolder,
                    )
                }
                AdaptNavigation(globalUiStateHolder = meApp.globalUiStateHolder)
            }
        }

        setContentView(root)

        lifecycleScope.launch {
            insetMutations().collect(meApp.globalUiStateHolder.accept)
        }
        lifecycleScope.launch {
            meApp.globalUiStateHolder.state
                .map { it.statusBarColor to it.navBarColor }
                .distinctUntilChanged()
                .collect { (statusBarColor, navBarColor) ->
                    window.statusBarColor = statusBarColor
                    window.navigationBarColor = navBarColor
                }
        }
    }
}

@Composable
private fun MainActivity.AdaptNavigation(globalUiStateHolder: GlobalUiStateHolder) {
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
        globalUiStateHolder.accept(mutation {
            copy(
                windowSizeClass = widthWindowSizeClass,
                navMode = when (widthWindowSizeClass) {
                    WindowSizeClass.COMPACT -> NavMode.BottomNav
                    WindowSizeClass.MEDIUM -> NavMode.NavRail
                    WindowSizeClass.EXPANDED -> NavMode.NavRail
                }
            )
        })
    }
}
