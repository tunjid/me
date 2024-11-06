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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.me.archiveedit.Action
import com.tunjid.me.archiveedit.ActualArchiveEditStateHolder
import com.tunjid.me.archiveedit.ArchiveEditRoute
import com.tunjid.me.archiveedit.ArchiveEditScreen
import com.tunjid.me.archiveedit.ArchiveEditStateHolderCreator
import com.tunjid.me.archiveedit.State
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.Message
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
    @Component val scaffoldComponent: InjectedScaffoldComponent
) {

    @IntoMap
    @Provides
    fun editRouteAdaptiveConfiguration(
        creator: ArchiveEditStateHolderCreator
    ) = EditRoutePattern to threePaneListDetailStrategy(
        paneMapping = { route ->
            mapOf(
                ThreePane.Primary to route,
                ThreePane.Secondary to route.children.first() as? Route,
            )
        },
        render = { route ->
            val stateHolder = creator.invoke(
                LocalLifecycleOwner.current.lifecycleScope,
                route,
            )
            val state = stateHolder.state.collectAsStateWithLifecycle().value
            ArchiveEditScreen(
                movableSharedElementScope = movableSharedElementScope(),
                state = state,
                actions = stateHolder.accept,
                modifier = Modifier.predictiveBackBackgroundModifier(paneScope = this),
            )
            GlobalUi(
                state = state,
                onAction = stateHolder.accept,
            )
        }
    )

    @IntoMap
    @Provides
    fun createRouteAdaptiveConfiguration(
        creator: ArchiveEditStateHolderCreator
    ) = CreateRoutePattern to threePaneListDetailStrategy(
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
                movableSharedElementScope = movableSharedElementScope(),
                state = state,
                actions = viewModel.accept,
                modifier = Modifier.predictiveBackBackgroundModifier(paneScope = this),
            )
            GlobalUi(
                state = state,
                onAction = viewModel.accept,
            )
        }
    )
}

@Composable
private fun PaneScope<ThreePane, *>.GlobalUi(
    state: State,
    onAction: (Action) -> Unit,
) {
    ScreenUiState(
        UiState(
            fabShows = true,
            fabText = if (state.upsert.id == null) "Create" else "Save",
            fabIcon = Icons.Default.Done,
            fabExtended = true,
            fabEnabled = !state.isSubmitting,
            fabClickListener = rememberUpdatedState { _: Unit ->
                onAction(
                    Action.Load.Submit(
                        kind = state.kind,
                        upsert = state.upsert,
                        headerPhoto = state.toUpload,
                    )
                )
            }.value,
            snackbarMessages = state.messages,
            snackbarMessageConsumer = rememberUpdatedState { it: Message ->
                onAction(Action.MessageConsumed(it))
            }.value,
            navVisibility = NavVisibility.GoneIfBottomNav,
            insetFlags = InsetFlags.NO_BOTTOM,
            statusBarColor = MaterialTheme.colorScheme.surface.toArgb(),
        )
    )
}