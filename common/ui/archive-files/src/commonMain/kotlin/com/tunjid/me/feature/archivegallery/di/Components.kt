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

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.feature.archivegallery.Action
import com.tunjid.me.feature.archivegallery.ActualArchiveGalleryStateHolder
import com.tunjid.me.feature.archivegallery.ArchiveGalleryRoute
import com.tunjid.me.feature.archivegallery.ArchiveGalleryScreen
import com.tunjid.me.feature.archivegallery.ArchiveGalleryStateHolderCreator
import com.tunjid.me.feature.archivegallery.State
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.SavedStateType
import com.tunjid.me.scaffold.di.routeAndMatcher
import com.tunjid.me.scaffold.scaffold.PaneScaffold
import com.tunjid.me.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.me.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.compose.threepane.transforms.requireThreePaneMovableSharedElementScope
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import kotlinx.serialization.modules.subclass
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides

private const val RoutePattern = "/archive/{id}/gallery"

internal val RouteParams.archiveId: ArchiveId
    get() = pathArgs.getValue("id").let(::ArchiveId)
internal val RouteParams.pageOffset
    get() = queryParams["offset"]?.firstOrNull()?.toIntOrNull() ?: 0
internal val RouteParams.urls
    get() = queryParams["url"] ?: emptyList()

@Component
abstract class ArchiveGalleryNavigationComponent {

    @IntoSet
    @Provides
    fun savedStateType(): SavedStateType = SavedStateType {
        subclass(State::class)
    }

    @IntoMap
    @Provides
    fun archiveFileRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
            routePattern = RoutePattern,
            routeMapper = ::ArchiveGalleryRoute,
        )
}

@Component
abstract class ArchiveGalleryScreenHolderComponent(
    @Component val dataComponent: InjectedDataComponent,
    @Component val scaffoldComponent: InjectedScaffoldComponent
) {

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        creator: ArchiveGalleryStateHolderCreator
    ) = RoutePattern to threePaneEntry(
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualArchiveGalleryStateHolder> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()
            PaneScaffold(
                modifier = Modifier
                    .predictiveBackBackgroundModifier(paneScope = this),
                showNavigation = true,
                onSnackBarMessageConsumed = {
                },
                topBar = {
                    PoppableDestinationTopAppBar {
                        viewModel.accept(Action.Navigate.Pop)
                    }
                },
                content = {
                    ArchiveGalleryScreen(
                        movableSharedElementScope = this,
                        state = state,
                        actions = viewModel.accept
                    )
                },
            )
        }
    )
}
