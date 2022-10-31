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

import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.scaffold.di.ScaffoldComponent
import com.tunjid.me.scaffold.di.SavedStateType
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.signin.*
import com.tunjid.treenav.strings.UrlRouteMatcher
import com.tunjid.treenav.strings.urlRouteMatcher
import kotlinx.serialization.modules.subclass
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides

@Component
abstract class SignInNavigationComponent {

    @IntoSet
    @Provides
    fun savedStatePolymorphicArg(): SavedStateType = SavedStateType {
        subclass(State::class)
    }

    @IntoSet
    @Provides
    fun profileRouteParser(): UrlRouteMatcher<AppRoute> = urlRouteMatcher(
        routePattern = "sign-in",
        routeMapper = { (route: String) ->
            SignInRoute(
                id = route,
            )
        }
    )
}

@Component
abstract class SignInScreenHolderComponent(
    @Component val dataComponent: InjectedDataComponent,
    @Component val scaffoldComponent: ScaffoldComponent
) {

    val ActualSignInMutator.bind: SignInMutator
        @Provides get() = this

    @IntoMap
    @Provides
    fun settingsMutatorCreator(
        assist: SignInMutatorCreator
    ): Pair<String, ScreenStateHolderCreator> = Pair(
        first = SignInRoute::class.simpleName!!,
        second = assist
    )
}