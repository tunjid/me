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

package com.tunjid.me.common.di

import com.tunjid.me.archivedetail.di.ArchiveDetailScreenHolderComponent
import com.tunjid.me.archiveedit.di.ArchiveEditScreenHolderComponent
import com.tunjid.me.core.di.SingletonScope
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.feature.archivegallery.di.ArchiveGalleryScreenHolderComponent
import com.tunjid.me.feature.archivefiles.di.ArchiveFilesScreenHolderComponent
import com.tunjid.me.feature.archivelist.di.ArchiveListScreenHolderComponent
import com.tunjid.me.profile.di.ProfileScreenHolderComponent
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.navigation.NavigationStateHolder
import com.tunjid.me.settings.di.SettingsScreenHolderComponent
import com.tunjid.me.signin.di.SignInScreenHolderComponent
import com.tunjid.me.sync.di.InjectedSyncComponent
import com.tunjid.scaffold.scaffold.MeAppState
import com.tunjid.treenav.compose.PaneStrategy
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.routeParserFrom
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@Component
abstract class AppScreenStateHolderComponent(
    @Component val scaffoldComponent: InjectedScaffoldComponent,
    @Component val syncComponent: InjectedSyncComponent,
    @Component val archiveListComponent: ArchiveListScreenHolderComponent,
    @Component val archiveDetailComponent: ArchiveDetailScreenHolderComponent,
    @Component val archiveEditComponent: ArchiveEditScreenHolderComponent,
    @Component val archiveGalleryComponent: ArchiveGalleryScreenHolderComponent,
    @Component val archiveFilesComponent: ArchiveFilesScreenHolderComponent,
    @Component val profileComponent: ProfileScreenHolderComponent,
    @Component val settingsComponent: SettingsScreenHolderComponent,
    @Component val signInComponent: SignInScreenHolderComponent,
) {

    abstract val routeConfigurationMap: Map<String, PaneStrategy<ThreePane, Route>>


    @Provides
    fun appState(
        navigationStateHolder: NavigationStateHolder,
        globalUiStateHolder: GlobalUiStateHolder,
    ): MeAppState = MeAppState(
        routeConfigurationMap = routeConfigurationMap,
        navigationStateHolder = navigationStateHolder,
        globalUiStateHolder = globalUiStateHolder,
    )

    abstract val app: PersistedMeApp
}
