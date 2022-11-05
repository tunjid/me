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
import com.tunjid.me.feature.archivelist.di.ArchiveListScreenHolderComponent
import com.tunjid.me.profile.di.ProfileScreenHolderComponent
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.settings.di.SettingsScreenHolderComponent
import com.tunjid.me.signin.di.SignInScreenHolderComponent
import com.tunjid.me.sync.di.InjectedSyncComponent
import kotlinx.coroutines.*
import me.tatarka.inject.annotations.Component

@Component
abstract class AppScreenStateHolderComponent(
    @Component val syncComponent: InjectedSyncComponent,
    @Component val archiveListComponent: ArchiveListScreenHolderComponent,
    @Component val archiveDetailComponent: ArchiveDetailScreenHolderComponent,
    @Component val archiveEditComponent: ArchiveEditScreenHolderComponent,
    @Component val profileComponent: ProfileScreenHolderComponent,
    @Component val settingsComponent: SettingsScreenHolderComponent,
    @Component val signInComponent: SignInScreenHolderComponent,
) {

    abstract val allScreenHolders: Map<String, ScreenStateHolderCreator>

    abstract val routeMutatorFactory: RouteMutatorFactory
}
