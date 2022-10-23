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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.tunjid.me.common.di.createAppDependencies
import com.tunjid.me.common.ui.theme.AppTheme
import com.tunjid.me.core.ui.dragdrop.PlatformDropTargetModifier
import com.tunjid.me.core.utilities.UriConverter
import com.tunjid.me.data.local.DatabaseDriverFactory
import com.tunjid.me.data.network.NetworkMonitor
import com.tunjid.me.feature.LocalRouteServiceLocator
import com.tunjid.me.scaffold.globalui.NavMode
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.scaffold.Scaffold
import com.tunjid.me.scaffold.permissions.PlatformPermissionsProvider
import com.tunjid.mutator.mutation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File

fun main() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val appDependencies = createAppDependencies(
        appScope = appScope,
        savedStatePath = savedStatePath(),
        initialUiState = UiState(navMode = NavMode.NavRail),
        permissionsProvider = PlatformPermissionsProvider(),
        database = AppDatabase(
            DatabaseDriverFactory(
                schema = AppDatabase.Schema,
            ).createDriver()
        ),
        networkMonitor = NetworkMonitor(scope = appScope),
        uriConverter = UriConverter(),
    )

    val scaffoldComponent = appDependencies.scaffoldComponent

    application {
        val windowState = rememberWindowState()
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Me as a composition"
        ) {
            val density = LocalDensity.current.density
            val dropParent = remember(density) {
                PlatformDropTargetModifier(
                    density = density,
                    window = window,
                )
            }
            AppTheme {
                Surface(
                    color = MaterialTheme.colors.background,
                ) {
                    CompositionLocalProvider(
                        LocalRouteServiceLocator provides appDependencies.routeServiceLocator,
                    ) {
                        Scaffold(
                            modifier = Modifier.then(dropParent),
                            component = appDependencies.scaffoldComponent,
                        )
                    }
                }
            }

            val currentWidth = windowState.size.width
            LaunchedEffect(currentWidth) {
                snapshotFlow { currentWidth < 600.dp }
                    .distinctUntilChanged()
                    .collect { isInPortrait ->
                        scaffoldComponent.uiActions(mutation {
                            copy(navMode = if (isInPortrait) NavMode.BottomNav else NavMode.NavRail)
                        })
                    }
            }
        }
    }
}

private fun savedStatePath(): Path = File(
    System.getProperty("java.io.tmpdir"),
    "tunji-me-saved-state-9.ser"
).toOkioPath()


