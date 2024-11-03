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

import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.SavedStateType
import com.tunjid.me.scaffold.di.routeAndMatcher
import com.tunjid.me.signin.SignInRoute
import com.tunjid.me.signin.SignInScreen
import com.tunjid.me.signin.SignInStateHolderCreator
import com.tunjid.me.signin.State
import com.tunjid.scaffold.scaffold.configuration.predictiveBackBackgroundModifier
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
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

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        creator: SignInStateHolderCreator
    ) = RoutePattern to threePaneListDetailStrategy(
        render = { route ->
            val stateHolder = creator.invoke(
                LocalLifecycleOwner.current.lifecycleScope,
                route,
            )
            SignInScreen(
                state = stateHolder.state.collectAsStateWithLifecycle().value,
                actions = stateHolder.accept,
                modifier = Modifier.predictiveBackBackgroundModifier(paneScope = this),
            )
        }
    )
}

@Component
abstract class SignInScreenHolderComponent(
    @Component val dataComponent: InjectedDataComponent,
    @Component val scaffoldComponent: InjectedScaffoldComponent
) {

//    val ActualSignInStateHolder.bind: SignInStateHolder
//        @Provides get() = this
//
//    @IntoMap
//    @Provides
//    fun settingsStateHolderCreator(
//        assist: SignInStateHolderCreator
//    ): Pair<String, ScreenStateHolderCreator> = Pair(
//        first = RoutePattern,
//        second = assist
//    )
}