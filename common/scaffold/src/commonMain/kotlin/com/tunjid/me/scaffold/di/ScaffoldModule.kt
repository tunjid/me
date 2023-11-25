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

package com.tunjid.me.scaffold.di

import com.tunjid.me.core.di.SingletonScope
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.core.utilities.fromBytes
import com.tunjid.me.scaffold.globalui.ActualGlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.lifecycle.ActualLifecycleStateHolder
import com.tunjid.me.scaffold.lifecycle.Lifecycle
import com.tunjid.me.scaffold.lifecycle.LifecycleStateHolder
import com.tunjid.me.scaffold.navigation.AppRoute
import com.tunjid.me.scaffold.navigation.NavigationMutation
import com.tunjid.me.scaffold.navigation.NavigationStateHolder
import com.tunjid.me.scaffold.navigation.PersistedNavigationStateHolder
import com.tunjid.me.scaffold.permissions.Permission
import com.tunjid.me.scaffold.permissions.Permissions
import com.tunjid.me.scaffold.permissions.PermissionsProvider
import com.tunjid.me.scaffold.savedstate.DataStoreSavedStateRepository
import com.tunjid.me.scaffold.savedstate.SavedStateRepository
import com.tunjid.mutator.Mutation
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.Route
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.UrlRouteMatcher
import com.tunjid.treenav.strings.routeParserFrom
import com.tunjid.treenav.strings.urlRouteMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import okio.Path

interface ScreenStateHolderCreator : (CoroutineScope, ByteArray?, AppRoute) -> Any

inline fun <reified Route : AppRoute> ((CoroutineScope, ByteArray?, Route) -> Any).downcast(): ScreenStateHolderCreator =
    object : ScreenStateHolderCreator {
        override fun invoke(scope: CoroutineScope, savedState: ByteArray?, route: AppRoute): Any =
            this@downcast(
                scope,
                savedState,
                route as Route
            )
    }

typealias SavedStateCache = (AppRoute) -> ByteArray?

fun <T : Route> routeAndMatcher(
    routePattern: String,
    routeMapper: (RouteParams) -> T
) = routePattern to urlRouteMatcher(
    routePattern = routePattern,
    routeMapper = routeMapper
)

/**
 * Wrapper for [ByteSerializer] to get around ksp compilation issues
 */
class ByteSerializerWrapper(byteSerializer: ByteSerializer) : ByteSerializer by byteSerializer

data class SavedStateType(
    val apply: PolymorphicModuleBuilder<ByteSerializable>.() -> Unit
)

class ScaffoldModule(
    val appScope: CoroutineScope,
    val savedStatePath: Path,
    val routeMatchers: List<UrlRouteMatcher<AppRoute>>,
    val permissionsProvider: PermissionsProvider,
    val byteSerializer: ByteSerializer,
)

inline fun <reified T : ByteSerializable> ByteSerializer.restoreState(savedState: ByteArray?): T? {
    return try {
        // Polymorphic serialization requires that the compile time type used to serialize, must also be used to
        // deserialize. See https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md
        if (savedState != null) fromBytes<ByteSerializable>(savedState) as? T else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@SingletonScope
@Component
abstract class InjectedScaffoldComponent(
    private val module: ScaffoldModule
) {
    @SingletonScope
    @Provides
    fun appScope(): CoroutineScope = module.appScope

    @SingletonScope
    @Provides
    fun savedStatePath(): Path = module.savedStatePath

    @SingletonScope
    @Provides
    fun byteSerializer(): ByteSerializer = module.byteSerializer

    @SingletonScope
    @Provides
    fun routeParser(): RouteParser<AppRoute> =
        routeParserFrom(*(module.routeMatchers).toTypedArray())

    @SingletonScope
    @Provides
    fun savedStateCache(
        savedStateRepository: SavedStateRepository
    ): SavedStateCache = { route ->
        savedStateRepository.savedState.value.routeStates[route.id]
    }

    @SingletonScope
    @Provides
    fun permissionsStateStream(
    ): StateFlow<Permissions> = module.permissionsProvider.stateHolder.state

    @SingletonScope
    @Provides
    fun permissionsActions(
    ): (Permission) -> Unit = module.permissionsProvider.stateHolder.accept

    @SingletonScope
    @Provides
    fun navStateStream(
        navStateHolder: NavigationStateHolder
    ): StateFlow<MultiStackNav> = navStateHolder.state

    @Provides
    fun navActions(): (NavigationMutation) -> Unit = navStateHolder.accept

    @SingletonScope
    @Provides
    fun globalUiStateStream(
        globalUiStateHolder: GlobalUiStateHolder
    ): StateFlow<UiState> = globalUiStateHolder.state

    @SingletonScope
    @Provides
    fun globalUiActions(
        globalUiStateHolder: GlobalUiStateHolder
    ): (Mutation<UiState>) -> Unit = globalUiStateHolder.accept

    @SingletonScope
    @Provides
    fun lifecycleStateStream(
        lifecycleStateHolder: LifecycleStateHolder
    ): StateFlow<Lifecycle> = lifecycleStateHolder.state

    val PersistedNavigationStateHolder.bind: NavigationStateHolder
        @SingletonScope
        @Provides get() = this

    val ActualGlobalUiStateHolder.bind: GlobalUiStateHolder
        @SingletonScope
        @Provides get() = this

    val ActualLifecycleStateHolder.bind: LifecycleStateHolder
        @SingletonScope
        @Provides get() = this

    val DataStoreSavedStateRepository.bind: SavedStateRepository
        @SingletonScope
        @Provides get() = this

    abstract val navStateHolder: NavigationStateHolder
}