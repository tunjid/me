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

package com.tunjid.me.feature.archivefiles.di

import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.feature.archivefiles.ActualArchiveFilesStateHolder
import com.tunjid.me.feature.archivefiles.ArchiveFilesParentRoute
import com.tunjid.me.feature.archivefiles.ArchiveFilesRoute
import com.tunjid.me.feature.archivefiles.ArchiveFilesStateHolder
import com.tunjid.me.feature.archivefiles.ArchiveFilesStateHolderCreator
import com.tunjid.me.feature.archivefiles.FileType
import com.tunjid.me.feature.archivefiles.State
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.SavedStateType
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.di.routeAndMatcher
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.treenav.strings.UrlRouteMatcher
import kotlinx.serialization.modules.subclass
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides

@Component
abstract class ArchiveFilesNavigationComponent {

    @IntoSet
    @Provides
    fun savedStateType(): SavedStateType = SavedStateType {
        subclass(State::class)
    }

    @IntoMap
    @Provides
    fun archiveFilesRouteParser(): Pair<String, UrlRouteMatcher<AppRoute>> =
        routeAndMatcher(
            routePattern = "archives/{kind}/{id}/files",
            routeMapper = { (
                                route: String,
                                pathKeys: Map<String, String>,
                                queryKeys: Map<String, List<String>>
                            ) ->
                val archiveId = ArchiveId(pathKeys["id"] ?: "")
                val kind = ArchiveKind.values().firstOrNull { it.type == pathKeys["kind"] }
                    ?: ArchiveKind.Articles
                val types = queryKeys["type"] ?: emptyList()
                when {
                    types.isEmpty() -> ArchiveFilesParentRoute(
                        id = route,
                        kind = kind,
                        archiveId = archiveId,
                        dndEnabled = queryKeys.dndEnabled(),
                        )

                    "image" in types -> ArchiveFilesRoute(
                        id = route,
                        kind = kind,
                        archiveId = archiveId,
                        dndEnabled = queryKeys.dndEnabled(),
                        fileType = FileType.Image
                    )

                    else -> ArchiveFilesRoute(
                        id = route,
                        kind = kind,
                        archiveId = archiveId,
                        dndEnabled = queryKeys.dndEnabled(),
                        fileType = FileType.Misc
                    )
                }
            }
        )

    private fun Map<String, List<String>>.dndEnabled() =
        (this["dndEnabled"]
            ?.map(String::toBooleanStrictOrNull)
            ?.any(true::equals)
            ?: false)
}

@Component
abstract class ArchiveFilesScreenHolderComponent(
    @Component val dataComponent: InjectedDataComponent,
    @Component val scaffoldComponent: InjectedScaffoldComponent
) {

    val ActualArchiveFilesStateHolder.bind: ArchiveFilesStateHolder
        @Provides get() = this

    @IntoMap
    @Provides
    fun archiveFilesStateHolderCreator(
        assist: ArchiveFilesStateHolderCreator
    ): Pair<String, ScreenStateHolderCreator> = Pair(
        first = ArchiveFilesRoute::class.simpleName!!,
        second = assist
    )
}