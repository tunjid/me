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

import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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
import com.tunjid.me.scaffold.scaffold.PaneBottomAppBar
import com.tunjid.me.scaffold.scaffold.PaneFab
import com.tunjid.me.scaffold.scaffold.PaneNavigationRail
import com.tunjid.me.scaffold.scaffold.PaneScaffold
import com.tunjid.me.scaffold.scaffold.UiTokens
import com.tunjid.me.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.me.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.treenav.compose.threepane.threePaneEntry
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
    @Component val scaffoldComponent: InjectedScaffoldComponent,
) {

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        creator: ArchiveListStateHolderCreator,
    ) = RoutePattern to threePaneEntry(
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualArchiveListStateHolder> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()
            rememberPaneScaffoldState().PaneScaffold(
                modifier = Modifier
                    .predictiveBackBackgroundModifier(paneScope = this),
                showNavigation = true,
                onSnackBarMessageConsumed = {
                },
                topBar = {
                    ScreenTopAppBar(
                        state = state,
                        actions = viewModel.accept,
                    )
                },
                content = { paddingValues ->
                    ArchiveListScreen(
                        movableSharedElementScope = this,
                        modifier = Modifier
                            .padding(
                                top = paddingValues.calculateTopPadding(),
                            )
                            .predictiveBackBackgroundModifier(paneScope = this),
                        state = state,
                        actions = viewModel.accept
                    )
                },
                floatingActionButton = {
                    if (!state.hasFetchedAuthStatus || state.isSignedIn) PaneFab(
                        modifier = Modifier,
                        text = "Create",
                        icon = Icons.Default.Add,
                        expanded = true,
                        onClick = {
                            viewModel.accept(Action.Navigate.Create(kind = state.queryState.currentQuery.kind))
                        },
                    )
                },
                navigationBar = {
                    PaneBottomAppBar()
                },
                navigationRail = {
                    PaneNavigationRail()
                },
            )
        }
    )
}


@Composable
private fun ScreenTopAppBar(
    state: State,
    actions: (Action) -> Unit,
) {
    TopAppBar(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.statusBars),
        title = {
            Text(
                text = state.queryState.currentQuery.kind.name,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
            )
        },
        actions = {
            IconButton(
                onClick = {
                    actions(Action.Fetch.QueryChange.ToggleOrder)
                },
                content = {
                    Icon(
                        imageVector =
                            if (state.queryState.currentQuery.desc) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                        contentDescription =
                            if (state.queryState.currentQuery.desc) "Ascending"
                            else "Descending",
                    )
                }
            )
            if (!state.isSignedIn) IconButton(
                onClick = {
                    actions(Action.Navigate.SignIn)
                },
                content = {
                    Icon(
                        imageVector = Icons.Default.AccountBox,
                        contentDescription = "Sign In",
                    )
                }
            )
        },
    )
}
