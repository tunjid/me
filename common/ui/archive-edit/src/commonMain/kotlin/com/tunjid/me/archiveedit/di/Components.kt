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

import androidx.compose.ui.Modifier
import com.tunjid.me.archiveedit.ActualArchiveEditStateHolder
import com.tunjid.me.archiveedit.ArchiveEditRoute
import com.tunjid.me.archiveedit.ArchiveEditScreen
import com.tunjid.me.archiveedit.ArchiveEditStateHolder
import com.tunjid.me.archiveedit.ArchiveEditStateHolderCreator
import com.tunjid.me.archiveedit.State
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.feature.rememberRetainedStateHolder
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.SavedStateType
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.di.routeAndMatcher
import com.tunjid.me.scaffold.lifecycle.collectAsStateWithLifecycle
import com.tunjid.me.scaffold.scaffold.backPreviewBackgroundModifier
import com.tunjid.scaffold.adaptive.adaptiveRouteConfiguration
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

    @IntoMap
    @Provides
    fun editRouteAdaptiveConfiguration() = EditRoutePattern to adaptiveRouteConfiguration(
        secondaryRoute = { route ->
            route.children.first() as? Route
        },
        render = { route ->
            val stateHolder = rememberRetainedStateHolder<ArchiveEditStateHolder>(
                route = route
            )
            ArchiveEditScreen(
                state = stateHolder.state.collectAsStateWithLifecycle().value,
                actions = stateHolder.accept,
                modifier = Modifier.backPreviewBackgroundModifier(),
            )
        }
    )

    @IntoMap
    @Provides
    fun createRouteAdaptiveConfiguration() = CreateRoutePattern to adaptiveRouteConfiguration(
        render = { route ->
            val stateHolder = rememberRetainedStateHolder<ArchiveEditStateHolder>(
                route = route
            )
            ArchiveEditScreen(
                state = stateHolder.state.collectAsStateWithLifecycle().value,
                actions = stateHolder.accept,
                modifier = Modifier.backPreviewBackgroundModifier(),
            )
        }
    )
}

@Component
abstract class ArchiveEditScreenHolderComponent(
    @Component val dataComponent: InjectedDataComponent,
    @Component val scaffoldComponent: InjectedScaffoldComponent
) {

    val ActualArchiveEditStateHolder.bind: ArchiveEditStateHolder
        @Provides get() = this

    @IntoMap
    @Provides
    fun archiveCreateStateHolderCreator(
        assist: ArchiveEditStateHolderCreator
    ): Pair<String, ScreenStateHolderCreator> = Pair(
        first = CreateRoutePattern,
        second = assist
    )

    @IntoMap
    @Provides
    fun archiveEditStateHolderCreator(
        assist: ArchiveEditStateHolderCreator
    ): Pair<String, ScreenStateHolderCreator> = Pair(
        first = EditRoutePattern,
        second = assist
    )
}