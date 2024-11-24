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

package com.tunjid.me.feature.archivefilesparent.di

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.feature.archivefilesparent.ActualArchiveFilesParentStateHolder
import com.tunjid.me.feature.archivefilesparent.ArchiveFilesParentRoute
import com.tunjid.me.feature.archivefilesparent.ArchiveFilesParentScreen
import com.tunjid.me.feature.archivefilesparent.ArchiveFilesParentStateHolderCreator
import com.tunjid.me.feature.archivefilesparent.State
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.SavedStateType
import com.tunjid.me.scaffold.di.routeAndMatcher
import com.tunjid.me.scaffold.globalui.NavVisibility
import com.tunjid.me.scaffold.globalui.ScreenUiState
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.scaffold.configuration.predictiveBackBackgroundModifier
import com.tunjid.treenav.compose.threepane.configurations.requireThreePaneMovableSharedElementScope

import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import kotlinx.serialization.modules.subclass
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides

private const val RoutePattern = "/archives/{kind}/{id}/files"

internal val RouteParams.archiveId get() = ArchiveId(pathArgs["id"] ?: "")
internal val RouteParams.kind
    get() = ArchiveKind.entries.firstOrNull { it.type == pathArgs["kind"] }
        ?: ArchiveKind.Articles

@Component
abstract class ArchiveFilesParentNavigationComponent {

    @IntoSet
    @Provides
    fun savedStateType(): SavedStateType = SavedStateType {
        subclass(State::class)
    }

    @IntoMap
    @Provides
    fun archiveFilesParentRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
            routePattern = RoutePattern,
            routeMapper = ::ArchiveFilesParentRoute
        )
}

@Component
abstract class ArchiveFilesParentScreenHolderComponent(
    @Component val dataComponent: InjectedDataComponent,
    @Component val scaffoldComponent: InjectedScaffoldComponent
) {

    @IntoMap
    @Provides
    fun filesParentRouteAdaptiveConfiguration(
        creator: ArchiveFilesParentStateHolderCreator
    ) = RoutePattern to threePaneListDetailStrategy(
        render = { route: Route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualArchiveFilesParentStateHolder> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()
            ArchiveFilesParentScreen(
                movableSharedElementScope = requireThreePaneMovableSharedElementScope(),
                modifier = Modifier.predictiveBackBackgroundModifier(paneScope = this),
                state = state,
                actions = viewModel.accept,
            )
            ScreenUiState(
                UiState(
                    fabShows = false,
                    fabExtended = true,
                    navVisibility = NavVisibility.Visible,
                    statusBarColor = MaterialTheme.colorScheme.surface.toArgb(),
                )
            )
        }
    )
}
