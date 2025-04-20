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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.me.archiveedit.Action
import com.tunjid.me.archiveedit.ActualArchiveEditStateHolder
import com.tunjid.me.archiveedit.ArchiveEditRoute
import com.tunjid.me.archiveedit.ArchiveEditScreen
import com.tunjid.me.archiveedit.ArchiveEditStateHolderCreator
import com.tunjid.me.archiveedit.State
import com.tunjid.me.archiveedit.headerThumbnail
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.ui.icons.Preview
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.SavedStateType
import com.tunjid.me.scaffold.di.routeAndMatcher
import com.tunjid.me.scaffold.scaffold.PaneBottomAppBar
import com.tunjid.me.scaffold.scaffold.PaneFab
import com.tunjid.me.scaffold.scaffold.PaneScaffold
import com.tunjid.me.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.me.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.compose.threepane.transforms.requireThreePaneMovableSharedElementScope
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import kotlinx.serialization.modules.subclass
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides

private const val EditRoutePattern = "/archives/{kind}/{id}/edit"

private const val CreateRoutePattern = "/archives/{kind}/create"

internal val RouteParams.archiveId: ArchiveId?
    get() = pathArgs["id"]?.let(::ArchiveId)

internal val RouteParams.kind: ArchiveKind
    get() = ArchiveKind.entries
        .firstOrNull { it.type == pathArgs["kind"] }
        ?: ArchiveKind.Articles

internal val RouteParams.archiveThumbnail: String?
    get() = queryParams["thumbnail"]?.firstOrNull()

@Component
abstract class ArchiveEditNavigationComponent {

    @IntoSet
    @Provides
    fun savedStateType(): SavedStateType = SavedStateType {
        subclass(State::class)
    }

    @IntoMap
    @Provides
    fun archiveEditRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
            routePattern = EditRoutePattern,
            routeMapper = ::ArchiveEditRoute,
        )

    @IntoMap
    @Provides
    fun archiveCreateRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
            routePattern = CreateRoutePattern,
            routeMapper = ::ArchiveEditRoute,
        )
}

@Component
abstract class ArchiveEditScreenHolderComponent(
    @Component val dataComponent: InjectedDataComponent,
    @Component val scaffoldComponent: InjectedScaffoldComponent,
) {

    @IntoMap
    @Provides
    fun editRouteAdaptiveConfiguration(
        creator: ArchiveEditStateHolderCreator,
    ) = EditRoutePattern to threePaneEntry<Route>(
        paneMapping = { route ->
            mapOf(
                ThreePane.Primary to route,
                ThreePane.Secondary to route.children.first() as? Route,
            )
        },
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualArchiveEditStateHolder> {
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
                topBar = {
                    TopAppBar(
                        state = state,
                        actions = viewModel.accept,
                    )
                },
                content = { paddingValues ->
                    ArchiveEditScreen(
                        movableSharedElementScope = requireThreePaneMovableSharedElementScope(),
                        modifier = Modifier
                            .padding(
                                top = paddingValues.calculateTopPadding(),
                            )
                            .predictiveBackBackgroundModifier(paneScope = this),
                        state = state,
                        actions = viewModel.accept
                    )
                    SecondaryPaneCloseBackHandler(
                        enabled = paneState.pane == ThreePane.Primary
                                && route.children.isNotEmpty()
                                && isMediumScreenWidthOrWider
                    )
                },
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                    viewModel.accept(Action.MessageConsumed(it))
                },
                floatingActionButton = {
                    PaneFab(
                        modifier = Modifier,
                        text = if (state.upsert.id == null) "Create" else "Save",
                        icon = Icons.Default.Done,
                        expanded = true,
                        onClick = {
                            viewModel.accept(
                                Action.Load.Submit(
                                    kind = state.kind,
                                    upsert = state.upsert,
                                    headerPhoto = state.toUpload,
                                )
                            )
                        },
                    )
                },
                navigationBar = {
                    PaneBottomAppBar()
                },
            )
        }
    )

    @IntoMap
    @Provides
    fun createRouteAdaptiveConfiguration(
        creator: ArchiveEditStateHolderCreator,
    ) = CreateRoutePattern to threePaneEntry(
        paneMapping = { route ->
            mapOf(
                ThreePane.Primary to route,
                ThreePane.Secondary to route.children.firstOrNull() as? Route,
            )
        },
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualArchiveEditStateHolder> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()
            ArchiveEditScreen(
                movableSharedElementScope = requireThreePaneMovableSharedElementScope(),
                state = state,
                actions = viewModel.accept,
                modifier = Modifier.predictiveBackBackgroundModifier(paneScope = this),
            )
        }
    )
}

@Composable
private fun TopAppBar(
    state: State,
    actions: (Action) -> Unit,
) {
    androidx.compose.material3.TopAppBar(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.statusBars),
        navigationIcon = {
            IconButton(
                modifier = Modifier
                    .size(56.dp)
                    .padding(16.dp),
                onClick = {
                    actions(Action.Navigate.Pop)
                },
                content = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            )
        },
        title = {
            Text(
                text = "${if (state.upsert.id == null) "Create" else "Edit"} ${state.kind.name}",
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
            )
        },
        actions = {
            if (state.isEditing) IconButton(
                modifier = Modifier
                    .size(56.dp)
                    .padding(start = 16.dp, end = 8.dp),
                onClick = {
                    actions(Action.ToggleEditView)
                },
                content = {
                    Icon(
                        imageVector = Icons.Default.Preview,
                        contentDescription = "Preview",
                    )
                }
            )
            if (!state.isEditing) IconButton(
                modifier = Modifier
                    .size(56.dp)
                    .padding(start = 16.dp, end = 8.dp),
                onClick = {
                    actions(Action.ToggleEditView)
                },
                content = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                    )
                }
            )
            IconButton(
                modifier = Modifier
                    .size(56.dp)
                    .padding(end = 16.dp, start = 8.dp),
                onClick = {
                    state.upsert.id?.let {
                        actions(
                            Action.Navigate.Files(
                                kind = state.kind,
                                archiveId = it,
                                thumbnail = state.headerThumbnail,
                            )
                        )
                    }
                },
                content = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Gallery",
                    )
                }
            )
        },
    )
}