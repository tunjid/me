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

import androidx.compose.ui.Modifier
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.feature.archivefiles.ActualArchiveFilesStateHolder
import com.tunjid.me.feature.archivefiles.ArchiveFilesParentRoute
import com.tunjid.me.feature.archivefiles.ArchiveFilesParentScreen
import com.tunjid.me.feature.archivefiles.ArchiveFilesRoute
import com.tunjid.me.feature.archivefiles.ArchiveFilesScreen
import com.tunjid.me.feature.archivefiles.ArchiveFilesStateHolder
import com.tunjid.me.feature.archivefiles.ArchiveFilesStateHolderCreator
import com.tunjid.me.feature.archivefiles.FileType
import com.tunjid.me.feature.archivefiles.State
import com.tunjid.me.feature.rememberRetainedStateHolder
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.SavedStateType
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.di.routeAndMatcher
import com.tunjid.me.scaffold.lifecycle.collectAsStateWithLifecycle
import com.tunjid.me.scaffold.scaffold.backPreviewBackgroundModifier
import com.tunjid.scaffold.adaptive.adaptiveRouteConfiguration
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteMatcher
import kotlinx.serialization.modules.subclass
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides

private const val FilesParentRoutePattern = "/archives/{kind}/{id}/files"
private const val FilesRoutePattern = "/archives/{kind}/{id}/files/{type}"

internal val RouteParams.archiveId get() = ArchiveId(pathArgs["id"] ?: "")
internal val RouteParams.kind
    get() = ArchiveKind.entries.firstOrNull { it.type == pathArgs["kind"] }
        ?: ArchiveKind.Articles

internal val RouteParams.dndEnabled
    get() = queryParams["dndEnabled"]
        ?.map(String::toBooleanStrictOrNull)
        ?.any(true::equals)
        ?: false

internal val RouteParams.urls get() = queryParams["url"] ?: emptyList()

internal val RouteParams.fileType: FileType
    get() {
        val type = pathArgs["type"]
        return when {
            type == null -> FileType.Misc
            "image" in type -> FileType.Image
            else -> FileType.Misc
        }
    }

@Component
abstract class ArchiveFilesNavigationComponent {

    @IntoSet
    @Provides
    fun savedStateType(): SavedStateType = SavedStateType {
        subclass(State::class)
    }

    @IntoMap
    @Provides
    fun archiveFilesParentRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
            routePattern = FilesParentRoutePattern,
            routeMapper = ::ArchiveFilesParentRoute
        )

    @IntoMap
    @Provides
    fun archiveFilesRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
            routePattern = FilesRoutePattern,
            routeMapper = ::ArchiveFilesRoute
        )

    @IntoMap
    @Provides
    fun filesParentRouteAdaptiveConfiguration() = FilesParentRoutePattern to adaptiveRouteConfiguration(
        render = { route ->
            ArchiveFilesParentScreen(
                modifier = Modifier.backPreviewBackgroundModifier(),
                children = route.children.filterIsInstance<ArchiveFilesRoute>(),
            )
        }
    )

    @IntoMap
    @Provides
    fun filesRouteAdaptiveConfiguration() = FilesRoutePattern to adaptiveRouteConfiguration(
        render = { route ->
            val stateHolder = rememberRetainedStateHolder<ArchiveFilesStateHolder>(
                route = route
            )
            ArchiveFilesScreen(
                state = stateHolder.state.collectAsStateWithLifecycle().value,
                actions = stateHolder.accept,
                modifier = Modifier.backPreviewBackgroundModifier(),
            )
        }
    )
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