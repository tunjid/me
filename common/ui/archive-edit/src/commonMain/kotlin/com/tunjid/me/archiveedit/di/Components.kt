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

package com.tunjid.me.archiveedit.di

import com.tunjid.me.archiveedit.*
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.scaffold.di.ScaffoldComponent
import com.tunjid.me.scaffold.di.SavedStateType
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.treenav.strings.UrlRouteMatcher
import com.tunjid.treenav.strings.urlRouteMatcher
import kotlinx.serialization.modules.subclass
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides

@Component
abstract class ArchiveEditNavigationComponent {

    @IntoSet
    @Provides
    fun savedStatePolymorphicArg(): SavedStateType = SavedStateType {
        subclass(State::class)
    }

    @IntoSet
    @Provides
    fun archiveEditRouteParser(): UrlRouteMatcher<AppRoute> = urlRouteMatcher(
        routePattern = "archives/{kind}/{id}/edit",
        routeMapper = { (route: String, pathKeys: Map<String, String>) ->
            val archiveId = ArchiveId(pathKeys["id"] ?: "")
            val kind = ArchiveKind.values().firstOrNull { it.type == pathKeys["kind"] } ?: ArchiveKind.Articles
            ArchiveEditRoute(
                id = route,
                kind = kind,
                archiveId = archiveId
            )
        }
    )

    @IntoSet
    @Provides
    fun archiveCreateRouteParser(): UrlRouteMatcher<AppRoute> = urlRouteMatcher(
        routePattern = "archives/{kind}/create",
        routeMapper = { (route, pathKeys) ->
            val kind = ArchiveKind.values().firstOrNull { it.type == pathKeys["kind"] } ?: ArchiveKind.Articles
            ArchiveEditRoute(
                id = route,
                kind = kind,
                archiveId = null
            )
        }
    )
}

@Component
abstract class ArchiveEditScreenHolderComponent(
    @Component val dataComponent: InjectedDataComponent,
    @Component val scaffoldComponent: ScaffoldComponent
) {

    val ActualArchiveEditMutator.bind: ArchiveEditMutator
        @Provides get() = this

    @IntoMap
    @Provides
    fun archiveListMutatorCreator(
        assist: ArchiveEditMutatorCreator
    ): Pair<String, ScreenStateHolderCreator> = Pair(
        first = ArchiveEditRoute::class.simpleName!!,
        second = assist
    )
}