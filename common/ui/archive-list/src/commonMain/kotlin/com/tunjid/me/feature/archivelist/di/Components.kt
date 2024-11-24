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

package com.tunjid.me.feature.archivelist.di

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.feature.archivelist.Action
import com.tunjid.me.feature.archivelist.ActualArchiveListStateHolder
import com.tunjid.me.feature.archivelist.ArchiveListRoute
import com.tunjid.me.feature.archivelist.ArchiveListScreen
import com.tunjid.me.feature.archivelist.ArchiveListStateHolderCreator
import com.tunjid.me.feature.archivelist.State
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.SavedStateType
import com.tunjid.me.scaffold.di.routeAndMatcher
import com.tunjid.me.scaffold.globalui.InsetFlags
import com.tunjid.me.scaffold.globalui.NavVisibility
import com.tunjid.me.scaffold.globalui.ScreenUiState
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.globalUi
import com.tunjid.me.scaffold.scaffold.configuration.predictiveBackBackgroundModifier
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.threepane.ThreePane
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

private const val RoutePattern = "/archives/{kind}"

internal val RouteParams.kind
    get() = ArchiveKind.entries.firstOrNull { it.type == pathArgs["kind"] } ?: ArchiveKind.Articles

@Component
abstract class ArchiveListNavigationComponent {

    @IntoSet
    @Provides
    fun savedStateType(): SavedStateType = SavedStateType {
        subclass(State::class)
    }

    @IntoMap
    @Provides
    fun archiveListRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
            routePattern = RoutePattern,
            routeMapper = ::ArchiveListRoute
        )
}

@Component
abstract class ArchiveListScreenHolderComponent(
    @Component val dataComponent: InjectedDataComponent,
    @Component val scaffoldComponent: InjectedScaffoldComponent
) {

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        creator: ArchiveListStateHolderCreator
    ) = RoutePattern to threePaneListDetailStrategy(
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualArchiveListStateHolder> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()
            ArchiveListScreen(
                movableSharedElementScope = requireThreePaneMovableSharedElementScope(),
                modifier = Modifier.predictiveBackBackgroundModifier(paneScope = this),
                state = state,
                actions = viewModel.accept
            )
            GlobalUi(
                state = state,
                onAction = viewModel.accept,
            )
        }
    )
}

@Composable
private fun PaneScope<ThreePane, Route>.GlobalUi(
    state: State,
    onAction: (Action) -> Unit
) {
    ScreenUiState(
        UiState(
            fabShows = if (state.hasFetchedAuthStatus) state.isSignedIn else globalUi.fabShows,
            fabExtended = true,
            fabText = "Create",
            fabIcon = Icons.Default.Add,
            fabClickListener = rememberUpdatedState { _: Unit ->
                onAction(Action.Navigate.Create(kind = state.queryState.currentQuery.kind))
            }.value,
            insetFlags = InsetFlags.NO_BOTTOM,
            navVisibility = NavVisibility.Visible,
            statusBarColor = MaterialTheme.colorScheme.surface.toArgb(),
        )
    )
}
