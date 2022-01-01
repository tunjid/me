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
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.WindowMetricsCalculator
import com.tunjid.me.common.component1
import com.tunjid.me.common.component2
import com.tunjid.me.common.globalui.GlobalUiMutator
import com.tunjid.me.common.globalui.NavMode
import com.tunjid.me.common.globalui.insetMutations
import com.tunjid.me.common.nav.pop
import com.tunjid.me.common.ui.scaffold.Root
import com.tunjid.me.common.ui.theme.AppTheme
import com.tunjid.mutator.accept
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = applicationContext as App
        val (navMutator, globalUiMutator) = app.appDeps.appMutator

        onBackPressedDispatcher.addCallback(this) {
            navMutator.accept { pop() }
        }

        setContent {
            AppTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Root(appDeps = app.appDeps)
                }
                adaptNavigation(globalUiMutator = globalUiMutator)
            }
        }

        lifecycleScope.launch {
            insetMutations().collect(globalUiMutator.accept)
        }
        lifecycleScope.launch {
            globalUiMutator.state
                .map { it.statusBarColor to it.navBarColor }
                .distinctUntilChanged()
                .collect { (statusBarColor, navBarColor) ->
                    window.statusBarColor = statusBarColor
                    window.navigationBarColor = navBarColor
                }
        }
    }
}

enum class WindowSizeClass { COMPACT, MEDIUM, EXPANDED }

@Composable
private fun MainActivity.adaptNavigation(globalUiMutator: GlobalUiMutator) {
    val configuration = LocalConfiguration.current
    val windowMetrics = remember(configuration) {
        WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(this)
    }
    val windowDpSize = with(LocalDensity.current) {
        windowMetrics.bounds.toComposeRect().size.toDpSize()
    }
    val widthWindowSizeClass = when {
        windowDpSize.width < 600.dp -> WindowSizeClass.COMPACT
        windowDpSize.width < 840.dp -> WindowSizeClass.MEDIUM
        else -> WindowSizeClass.EXPANDED
    }

//    val heightWindowSizeClass = when {
//        windowDpSize.height < 480.dp -> WindowSizeClass.COMPACT
//        windowDpSize.height < 900.dp -> WindowSizeClass.MEDIUM
//        else -> WindowSizeClass.EXPANDED
//    }

    LaunchedEffect(widthWindowSizeClass) {
        globalUiMutator.accept {
            copy(
                navMode = when (widthWindowSizeClass) {
                    WindowSizeClass.COMPACT -> NavMode.BottomNav
                    WindowSizeClass.MEDIUM -> NavMode.NavRail
                    WindowSizeClass.EXPANDED -> NavMode.NavRail
                }
            )
        }
    }
}