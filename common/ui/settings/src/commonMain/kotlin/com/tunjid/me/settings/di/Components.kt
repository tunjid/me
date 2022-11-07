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

import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.SavedStateType
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.di.routeAndMatcher
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.settings.ActualSettingsMutator
import com.tunjid.me.settings.SettingsMutator
import com.tunjid.me.settings.SettingsMutatorCreator
import com.tunjid.me.settings.SettingsRoute
import com.tunjid.me.settings.State
import com.tunjid.treenav.strings.UrlRouteMatcher
import com.tunjid.treenav.strings.urlRouteMatcher
import kotlinx.serialization.modules.subclass
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides

@Component
abstract class SettingsNavigationComponent {

    @IntoSet
    @Provides
    fun savedStateType(): SavedStateType = SavedStateType {
        subclass(State::class)
    }

    @IntoMap
    @Provides
    fun settingsRouteParser(): Pair<String, UrlRouteMatcher<AppRoute>> =
        routeAndMatcher(
            routePattern = "settings",
            routeMapper = { (route: String) ->
                SettingsRoute(
                    id = route,
                )
            }
        )
}

@Component
abstract class SettingsScreenHolderComponent(
    @Component val dataComponent: InjectedDataComponent,
    @Component val scaffoldComponent: InjectedScaffoldComponent
) {

    val ActualSettingsMutator.bind: SettingsMutator
        @Provides get() = this

    @IntoMap
    @Provides
    fun settingsMutatorCreator(
        assist: SettingsMutatorCreator
    ): Pair<String, ScreenStateHolderCreator> = Pair(
        first = SettingsRoute::class.simpleName!!,
        second = assist
    )
}