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
import com.tunjid.me.archivedetail.di.ArchiveDetailNavigationComponent
import com.tunjid.me.archivedetail.di.ArchiveDetailScreenHolderComponent
import com.tunjid.me.archivedetail.di.create
import com.tunjid.me.archiveedit.di.ArchiveEditNavigationComponent
import com.tunjid.me.archiveedit.di.ArchiveEditScreenHolderComponent
import com.tunjid.me.archiveedit.di.create
import com.tunjid.me.common.di.AppRouteComponent
import com.tunjid.me.common.di.AppScreenStateHolderComponent
import com.tunjid.me.common.di.create
import com.tunjid.me.common.ui.theme.AppTheme
import com.tunjid.me.core.ui.dragdrop.PlatformDropTargetModifier
import com.tunjid.me.core.utilities.ActualUriConverter
import com.tunjid.me.data.di.DataModule
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.data.di.create
import com.tunjid.me.data.local.DatabaseDriverFactory
import com.tunjid.me.feature.LocalScreenStateHolderCache
import com.tunjid.me.feature.archivelist.di.ArchiveListNavigationComponent
import com.tunjid.me.feature.archivelist.di.ArchiveListScreenHolderComponent
import com.tunjid.me.feature.archivelist.di.create
import com.tunjid.me.profile.di.ProfileNavigationComponent
import com.tunjid.me.profile.di.ProfileScreenHolderComponent
import com.tunjid.me.profile.di.create
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.ScaffoldModule
import com.tunjid.me.scaffold.di.create
import com.tunjid.me.scaffold.globalui.scaffold.Scaffold
import com.tunjid.me.scaffold.permissions.PlatformPermissionsProvider
import com.tunjid.me.settings.di.SettingsNavigationComponent
import com.tunjid.me.settings.di.SettingsScreenHolderComponent
import com.tunjid.me.settings.di.create
import com.tunjid.me.signin.di.SignInNavigationComponent
import com.tunjid.me.signin.di.SignInScreenHolderComponent
import com.tunjid.me.signin.di.create
import com.tunjid.me.sync.di.InjectedSyncComponent
import com.tunjid.me.sync.di.SyncModule
import com.tunjid.me.sync.di.create
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File
import com.tunjid.me.data.network.NetworkMonitor
import com.tunjid.me.scaffold.globalui.NavMode
import com.tunjid.mutator.mutation

fun main() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val appRouteComponent = AppRouteComponent::class.create(
        archiveListNavigationComponent = ArchiveListNavigationComponent::class.create(),
        archiveDetailNavigationComponent = ArchiveDetailNavigationComponent::class.create(),
        archiveEditNavigationComponent = ArchiveEditNavigationComponent::class.create(),
        profileNavigationComponent = ProfileNavigationComponent::class.create(),
        settingsNavigationComponent = SettingsNavigationComponent::class.create(),
        signInNavigationComponent = SignInNavigationComponent::class.create(),
    )

    val appDatabase = AppDatabase(
        DatabaseDriverFactory(
            schema = AppDatabase.Schema,
        ).createDriver()
    )

    val injectedDataComponent = InjectedDataComponent::class.create(
        DataModule(
            database = appDatabase,
            uriConverter = ActualUriConverter(),
        )
    )

    val injectedScaffoldComponent = InjectedScaffoldComponent::class.create(
        ScaffoldModule(
            appScope = appScope,
            savedStatePath = savedStatePath(),
            permissionsProvider = PlatformPermissionsProvider(),
            uriConverter = ActualUriConverter(),
            routeMatchers = appRouteComponent.allUrlRouteMatchers.toList(),
            byteSerializer = appRouteComponent.byteSerializer,
        )
    )

    val appScreenStateHolderComponent = AppScreenStateHolderComponent::class.create(
        syncComponent = InjectedSyncComponent::class.create(
            module = SyncModule(
                appScope = appScope,
                networkMonitor = NetworkMonitor(appScope),
                database = appDatabase,
            ),
            dataComponent = injectedDataComponent
        ),
        archiveListComponent = ArchiveListScreenHolderComponent::class.create(
            scaffoldComponent = injectedScaffoldComponent,
            dataComponent = injectedDataComponent,
        ),
        archiveDetailComponent = ArchiveDetailScreenHolderComponent::class.create(
            scaffoldComponent = injectedScaffoldComponent,
            dataComponent = injectedDataComponent,
        ),
        archiveEditComponent = ArchiveEditScreenHolderComponent::class.create(
            scaffoldComponent = injectedScaffoldComponent,
            dataComponent = injectedDataComponent,
        ),
        profileComponent = ProfileScreenHolderComponent::class.create(
            scaffoldComponent = injectedScaffoldComponent,
            dataComponent = injectedDataComponent,
        ),
        settingsComponent = SettingsScreenHolderComponent::class.create(
            scaffoldComponent = injectedScaffoldComponent,
            dataComponent = injectedDataComponent,
        ),
        signInComponent = SignInScreenHolderComponent::class.create(
            scaffoldComponent = injectedScaffoldComponent,
            dataComponent = injectedDataComponent,
        ),
    )

    val app = appScreenStateHolderComponent.app

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
                        LocalScreenStateHolderCache provides app,
                    ) {
                        Scaffold(
                            modifier = Modifier.then(dropParent),
                            navMutator = app.navMutator,
                            globalUiMutator = app.globalUiMutator,
                        )
                    }
                }
            }

            val currentWidth = windowState.size.width
            LaunchedEffect(currentWidth) {
                snapshotFlow { currentWidth < 600.dp }
                    .distinctUntilChanged()
                    .collect { isInPortrait ->
                        app.globalUiMutator.accept(mutation {
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


