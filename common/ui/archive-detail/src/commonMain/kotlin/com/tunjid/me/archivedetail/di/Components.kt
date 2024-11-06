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

package com.tunjid.me.archivedetail.di

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import com.tunjid.me.archivedetail.Action
import com.tunjid.me.archivedetail.ActualArchiveDetailStateHolder
import com.tunjid.me.archivedetail.ArchiveDetailRoute
import com.tunjid.me.archivedetail.ArchiveDetailScreen
import com.tunjid.me.archivedetail.ArchiveDetailStateHolderCreator
import com.tunjid.me.archivedetail.State
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.SavedStateType
import com.tunjid.me.scaffold.di.routeAndMatcher
import com.tunjid.me.scaffold.globalui.InsetFlags
import com.tunjid.me.scaffold.globalui.NavVisibility
import com.tunjid.me.scaffold.globalui.ScreenUiState
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.scaffold.configuration.predictiveBackBackgroundModifier
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.configurations.movableSharedElementScope
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import kotlinx.serialization.modules.subclass
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides

private const val RoutePattern = "/archives/{kind}/{id}"

internal val RouteParams.archiveId: ArchiveId?
    get() = pathArgs["id"]?.let(::ArchiveId)

internal val RouteParams.kind
    get() = ArchiveKind.entries.firstOrNull { it.type == pathArgs["kind"] }
        ?: ArchiveKind.Articles

internal val RouteParams.archiveThumbnail: String?
    get() = queryParams["thumbnail"]?.firstOrNull()

@Component
abstract class ArchiveDetailNavigationComponent {

    @IntoSet
    @Provides
    fun savedStateType(): SavedStateType = SavedStateType {
        subclass(State::class)
    }

    @IntoMap
    @Provides
    fun archiveDetailRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
            routePattern = RoutePattern,
            routeMapper = ::ArchiveDetailRoute
        )
}

@Component
abstract class ArchiveDetailScreenHolderComponent(
    @Component val dataComponent: InjectedDataComponent,
    @Component val scaffoldComponent: InjectedScaffoldComponent
) {

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        creator: ArchiveDetailStateHolderCreator
    ) = RoutePattern to threePaneListDetailStrategy(
        paneMapping = { route ->
            mapOf(
                ThreePane.Primary to route,
                ThreePane.Secondary to route.children.first() as? Route,
            )
        },
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualArchiveDetailStateHolder> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()
            ArchiveDetailScreen(
                movableSharedElementScope = movableSharedElementScope(),
                modifier = Modifier.predictiveBackBackgroundModifier(paneScope = this),
                state = state,
                actions = viewModel.accept
            )
            GlobalUi(
                state = state,
                actions = viewModel.accept
            )
        }
    )

}

@Composable
private fun PaneScope<ThreePane, Route>.GlobalUi(state: State, actions: (Action) -> Unit) {
    ScreenUiState(
        UiState(
            navVisibility = NavVisibility.Visible,
            // Prevents UI from jittering as load starts
//            fabShows = if (state.hasFetchedAuthStatus) state.canEdit else currentUiState.fabShows,
            fabExtended = true,
            fabText = "Edit",
            fabIcon = Icons.Default.Edit,
            fabClickListener = rememberUpdatedState { _: Unit ->
                val archiveId = state.archive?.id
                if (archiveId != null) actions(
                    Action.Navigate.Edit(
                        kind = state.kind,
                        archiveId = state.archive.id,
                        thumbnail = state.archive.thumbnail,
                    )
                )
            }.value,
            insetFlags = InsetFlags.NO_BOTTOM,
            statusBarColor = MaterialTheme.colorScheme.surface.toArgb(),
        )
    )
}
