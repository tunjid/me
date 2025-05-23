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

package com.tunjid.me.settings.di

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
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.SavedStateType
import com.tunjid.me.scaffold.di.routeAndMatcher
import com.tunjid.me.scaffold.scaffold.PaneScaffold
import com.tunjid.me.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.me.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.me.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.me.settings.Action
import com.tunjid.me.settings.ActualSettingsStateHolder
import com.tunjid.me.settings.SettingsRoute
import com.tunjid.me.settings.SettingsScreen
import com.tunjid.me.settings.SettingsStateHolderCreator
import com.tunjid.me.settings.State
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.RouteMatcher
import kotlinx.serialization.modules.subclass
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides

private const val RoutePattern = "/settings"

@Component
abstract class SettingsNavigationComponent {

    @IntoSet
    @Provides
    fun savedStateType(): SavedStateType = SavedStateType {
        subclass(State::class)
    }

    @IntoMap
    @Provides
    fun settingsRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
            routePattern = RoutePattern,
            routeMapper = ::SettingsRoute,
        )
}

@Component
abstract class SettingsScreenHolderComponent(
    @Component val dataComponent: InjectedDataComponent,
    @Component val scaffoldComponent: InjectedScaffoldComponent
) {

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        creator: SettingsStateHolderCreator,
    ) = RoutePattern to threePaneEntry(
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualSettingsStateHolder> {
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
                    PoppableDestinationTopAppBar(
                        title = {
                            Text(
                                text = "Settings",
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                            )
                        }
                    ) {
                        viewModel.accept(Action.Navigate.Pop)
                    }
                },
                content = { paddingValues ->
                    SettingsScreen(
                        modifier = Modifier
                            .padding(paddingValues)
                            .predictiveBackBackgroundModifier(paneScope = this),
                        state = state,
                        actions = viewModel.accept
                    )
                },
            )
        }
    )
}