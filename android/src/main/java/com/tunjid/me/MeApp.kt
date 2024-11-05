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

import android.content.Context
import com.tunjid.me.archivedetail.di.ArchiveDetailNavigationComponent
import com.tunjid.me.archivedetail.di.ArchiveDetailScreenHolderComponent
import com.tunjid.me.archivedetail.di.create
import com.tunjid.me.archiveedit.di.ArchiveEditNavigationComponent
import com.tunjid.me.archiveedit.di.ArchiveEditScreenHolderComponent
import com.tunjid.me.archiveedit.di.create
import com.tunjid.me.common.di.AppRouteComponent
import com.tunjid.me.common.di.allRouteMatchers
import com.tunjid.me.common.di.AppScreenStateHolderComponent
import com.tunjid.me.common.di.create
import com.tunjid.me.core.utilities.ActualUriConverter
import com.tunjid.me.data.di.DataModule
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.data.di.create
import com.tunjid.me.data.local.DatabaseDriverFactory
import com.tunjid.me.data.local.databaseDispatcher
import com.tunjid.me.data.network.NetworkMonitor
import com.tunjid.me.common.di.MeApp
import com.tunjid.me.feature.archivegallery.di.ArchiveGalleryNavigationComponent
import com.tunjid.me.feature.archivegallery.di.ArchiveGalleryScreenHolderComponent
import com.tunjid.me.feature.archivefiles.di.ArchiveFilesNavigationComponent
import com.tunjid.me.feature.archivefiles.di.ArchiveFilesScreenHolderComponent
import com.tunjid.me.feature.archivefiles.di.create
import com.tunjid.me.feature.archivefilesparent.di.ArchiveFilesParentNavigationComponent
import com.tunjid.me.feature.archivefilesparent.di.ArchiveFilesParentScreenHolderComponent
import com.tunjid.me.feature.archivegallery.di.create
import com.tunjid.me.feature.archivelist.di.ArchiveListNavigationComponent
import com.tunjid.me.feature.archivelist.di.ArchiveListScreenHolderComponent
import com.tunjid.me.feature.archivelist.di.create
import com.tunjid.me.profile.di.ProfileNavigationComponent
import com.tunjid.me.profile.di.ProfileScreenHolderComponent
import com.tunjid.me.profile.di.create
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.ScaffoldModule
import com.tunjid.me.scaffold.di.create
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
import okio.Path
import okio.Path.Companion.toPath

fun createMeApp(context: Context): MeApp {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val appRouteComponent = AppRouteComponent::class.create(
        archiveListNavigationComponent = ArchiveListNavigationComponent::class.create(),
        archiveDetailNavigationComponent = ArchiveDetailNavigationComponent::class.create(),
        archiveEditNavigationComponent = ArchiveEditNavigationComponent::class.create(),
        archiveGalleryNavigationComponent = ArchiveGalleryNavigationComponent::class.create(),
        archiveFilesParentNavigationComponent = ArchiveFilesParentNavigationComponent::class.create(),
        archiveFilesNavigationComponent = ArchiveFilesNavigationComponent::class.create(),
        profileNavigationComponent = ProfileNavigationComponent::class.create(),
        settingsNavigationComponent = SettingsNavigationComponent::class.create(),
        signInNavigationComponent = SignInNavigationComponent::class.create(),
    )

    val appDatabase = AppDatabase(
        DatabaseDriverFactory(
            context = context,
            schema = AppDatabase.Schema,
        ).createDriver()
    )

    val injectedDataComponent = InjectedDataComponent::class.create(
        DataModule(
            database = appDatabase,
            uriConverter = ActualUriConverter(
                context = context,
                dispatcher = databaseDispatcher()
            ),
        )
    )

    val injectedScaffoldComponent = InjectedScaffoldComponent::class.create(
        ScaffoldModule(
            appScope = appScope,
            savedStatePath = context.savedStatePath(),
            permissionsProvider = PlatformPermissionsProvider(
                appScope = appScope,
                context = context
            ),
            routeMatchers = appRouteComponent.allRouteMatchers.toList(),
            byteSerializer = appRouteComponent.byteSerializer,
        )
    )

    val appScreenStateHolderComponent = AppScreenStateHolderComponent::class.create(
        scaffoldComponent = injectedScaffoldComponent,
        syncComponent = InjectedSyncComponent::class.create(
            module = SyncModule(
                appScope = appScope,
                networkMonitor = NetworkMonitor(
                    scope = appScope,
                    context = context
                ),
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
        archiveGalleryComponent = ArchiveGalleryScreenHolderComponent::class.create(
            scaffoldComponent = injectedScaffoldComponent,
            dataComponent = injectedDataComponent,
        ),
        archiveFilesParentComponent = ArchiveFilesParentScreenHolderComponent::class.create(
            scaffoldComponent = injectedScaffoldComponent,
            dataComponent = injectedDataComponent,
        ),
        archiveFilesComponent = ArchiveFilesScreenHolderComponent::class.create(
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

    return appScreenStateHolderComponent.app
}

private fun Context.savedStatePath(): Path =
    filesDir.resolve("savedState").absolutePath.toPath()