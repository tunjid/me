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

package com.tunjid.me.signin.di

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.tunjid.me.scaffold.scaffold.PaneFab
import com.tunjid.me.scaffold.scaffold.PaneScaffold
import com.tunjid.me.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.me.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.me.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.me.signin.Action
import com.tunjid.me.signin.ActualSignInStateHolder
import com.tunjid.me.signin.SignInRoute
import com.tunjid.me.signin.SignInScreen
import com.tunjid.me.signin.SignInStateHolderCreator
import com.tunjid.me.signin.State
import com.tunjid.me.signin.sessionRequest
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.RouteMatcher
import kotlinx.serialization.modules.subclass
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides

private const val RoutePattern = "/sign-in"

@Component
abstract class SignInNavigationComponent {

    @IntoSet
    @Provides
    fun savedStateType(): SavedStateType = SavedStateType {
        subclass(State::class)
    }

    @IntoMap
    @Provides
    fun profileRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
            routePattern = RoutePattern,
            routeMapper = ::SignInRoute,
        )

}

@Component
abstract class SignInScreenHolderComponent(
    @Component val dataComponent: InjectedDataComponent,
    @Component val scaffoldComponent: InjectedScaffoldComponent
) {

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        creator: SignInStateHolderCreator
    ) = RoutePattern to threePaneEntry(
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualSignInStateHolder> {
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
                topBar = {
                    PoppableDestinationTopAppBar(
                        title = {
                            Text(
                                text = "SignIn",
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
                    SignInScreen(
                        modifier = Modifier
                            .padding(paddingValues)
                            .predictiveBackBackgroundModifier(paneScope = this),
                        state = state,
                        actions = viewModel.accept
                    )
                },
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                    viewModel.accept(Action.MessageConsumed(it))
                },
                floatingActionButton = {
                    PaneFab(
                        modifier = Modifier,
                        text = "Submit",
                        icon = Icons.Default.Check,
                        expanded = true,
                        onClick = {
                            viewModel.accept(
                                Action.Submit(request = state.sessionRequest)
                            )
                        },
                    )
                },
            )
        }
    )
}