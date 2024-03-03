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

package com.tunjid.me.feature.archivelist.di

import androidx.compose.ui.Modifier
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.feature.archivelist.ActualArchiveListStateHolder
import com.tunjid.me.feature.archivelist.ArchiveListRoute
import com.tunjid.me.feature.archivelist.ArchiveListScreen
import com.tunjid.me.feature.archivelist.ArchiveListStateHolder
import com.tunjid.me.feature.archivelist.ArchiveListStateHolderCreator
import com.tunjid.me.feature.archivelist.State
import com.tunjid.me.feature.rememberRetainedStateHolder
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.SavedStateType
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.di.routeAndMatcher
import com.tunjid.me.scaffold.lifecycle.collectAsStateWithLifecycle
import com.tunjid.me.scaffold.scaffold.backPreviewBackgroundModifier
import com.tunjid.scaffold.adaptive.adaptiveRouteConfiguration
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import kotlinx.serialization.modules.subclass
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides

private const val RoutePattern = "/archives/{kind}"

internal val RouteParams.kind
    get() = ArchiveKind.entries.firstOrNull { it.type == pathArgs["kind"] } ?: ArchiveKind.Articles

@Component
abstract class ArchiveListNavigationComponent {

    @IntoSet
    @Provides
    fun savedStateType(): SavedStateType = SavedStateType {
        subclass(State::class)
    }

    @IntoMap
    @Provides
    fun archiveListRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
            routePattern = RoutePattern,
            routeMapper = ::ArchiveListRoute
        )

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration() = RoutePattern to adaptiveRouteConfiguration(
        render = { route ->
            val stateHolder = rememberRetainedStateHolder<ArchiveListStateHolder>(
                route = route
            )
            ArchiveListScreen(
                modifier = Modifier.backPreviewBackgroundModifier(),
                state = stateHolder.state.collectAsStateWithLifecycle().value,
                actions = stateHolder.accept
            )
        }
    )
}

@Component
abstract class ArchiveListScreenHolderComponent(
    @Component val dataComponent: InjectedDataComponent,
    @Component val scaffoldComponent: InjectedScaffoldComponent
) {

    val ActualArchiveListStateHolder.bind: ArchiveListStateHolder
        @Provides get() = this

    @IntoMap
    @Provides
    fun archiveListStateHolderCreator(
        assist: ArchiveListStateHolderCreator
    ): Pair<String, ScreenStateHolderCreator> = Pair(
        first = ArchiveListRoute::class.simpleName!!,
        second = assist
    )
}