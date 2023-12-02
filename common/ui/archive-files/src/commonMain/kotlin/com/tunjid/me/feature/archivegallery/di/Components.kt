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

package com.tunjid.me.feature.archivegallery.di

import com.tunjid.me.core.model.ArchiveFileId
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.feature.archivegallery.ActualArchiveGalleryStateHolder
import com.tunjid.me.feature.archivegallery.ArchiveGalleryRoute
import com.tunjid.me.feature.archivegallery.ArchiveGalleryStateHolder
import com.tunjid.me.feature.archivegallery.ArchiveGalleryStateHolderCreator
import com.tunjid.me.feature.archivegallery.State
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.SavedStateType
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.di.routeAndMatcher
import com.tunjid.me.scaffold.navigation.AppRoute
import com.tunjid.treenav.strings.UrlRouteMatcher
import kotlinx.serialization.modules.subclass
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides

@Component
abstract class ArchiveGalleryNavigationComponent {

    @IntoSet
    @Provides
    fun savedStateType(): SavedStateType = SavedStateType {
        subclass(State::class)
    }

    @IntoMap
    @Provides
    fun archiveFileRouteParser(): Pair<String, UrlRouteMatcher<AppRoute>> =
        routeAndMatcher(
            routePattern = "archive/{id}/gallery",
            routeMapper = { (
                                route: String,
                                pathKeys: Map<String, String>,
                                queryParams: Map<String, List<String>>,
                            ) ->
                ArchiveGalleryRoute(
                    route = route,
                    pathArgs = pathKeys,
                    queryArgs = queryParams
                )
            }
        )
}

@Component
abstract class ArchiveGalleryScreenHolderComponent(
    @Component val dataComponent: InjectedDataComponent,
    @Component val scaffoldComponent: InjectedScaffoldComponent
) {

    val ActualArchiveGalleryStateHolder.bind: ArchiveGalleryStateHolder
        @Provides get() = this

    @IntoMap
    @Provides
    fun archiveFileStateHolderCreator(
        assist: ArchiveGalleryStateHolderCreator
    ): Pair<String, ScreenStateHolderCreator> = Pair(
        first = ArchiveGalleryRoute::class.simpleName!!,
        second = assist
    )
}