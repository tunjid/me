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

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.feature.archivefilesparent.Action
import com.tunjid.me.feature.archivefilesparent.ActualArchiveFilesParentStateHolder
import com.tunjid.me.feature.archivefilesparent.ArchiveFilesParentRoute
import com.tunjid.me.feature.archivefilesparent.ArchiveFilesParentScreen
import com.tunjid.me.feature.archivefilesparent.ArchiveFilesParentStateHolderCreator
import com.tunjid.me.feature.archivefilesparent.State
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.SavedStateType
import com.tunjid.me.scaffold.di.routeAndMatcher
import com.tunjid.me.scaffold.scaffold.PaneBottomAppBar
import com.tunjid.me.scaffold.scaffold.PaneScaffold
import com.tunjid.me.scaffold.scaffold.PoppableDestinationTopAppBar
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
    ) = RoutePattern to threePaneEntry(
        render = { route: Route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualArchiveFilesParentStateHolder> {
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
                    PoppableDestinationTopAppBar(
                        title = {
                            Text(
                                text = "Files",
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                            )
                        },
                        onBackPressed = {
                            viewModel.accept(Action.Navigate.Pop)
                        },
                    )
                },
                content = { paddingValues ->
                    ArchiveFilesParentScreen(
                        movableSharedElementScope = this,
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
                navigationBar = {
                    PaneBottomAppBar()
                },
            )
        }
    )
}
