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

import androidx.compose.ui.Modifier
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.feature.rememberRetainedStateHolder
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.SavedStateType
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.di.routeAndMatcher
import com.tunjid.me.scaffold.lifecycle.collectAsStateWithLifecycle
import com.tunjid.me.scaffold.scaffold.backPreviewBackgroundModifier
import com.tunjid.me.settings.ActualSettingsStateHolder
import com.tunjid.me.settings.SettingsRoute
import com.tunjid.me.settings.SettingsScreen
import com.tunjid.me.settings.SettingsStateHolder
import com.tunjid.me.settings.SettingsStateHolderCreator
import com.tunjid.me.settings.State
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
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

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration() = RoutePattern to threePaneListDetailStrategy(
        render = { route ->
            val stateHolder = rememberRetainedStateHolder<SettingsStateHolder>(
                route = route
            )
            SettingsScreen(
                state = stateHolder.state.collectAsStateWithLifecycle().value,
                actions = stateHolder.accept,
                modifier = Modifier.backPreviewBackgroundModifier(),
            )
        }
    )
}

@Component
abstract class SettingsScreenHolderComponent(
    @Component val dataComponent: InjectedDataComponent,
    @Component val scaffoldComponent: InjectedScaffoldComponent
) {

    val ActualSettingsStateHolder.bind: SettingsStateHolder
        @Provides get() = this

    @IntoMap
    @Provides
    fun settingsStateHolderCreator(
        assist: SettingsStateHolderCreator
    ): Pair<String, ScreenStateHolderCreator> = Pair(
        first = RoutePattern,
        second = assist
    )
}