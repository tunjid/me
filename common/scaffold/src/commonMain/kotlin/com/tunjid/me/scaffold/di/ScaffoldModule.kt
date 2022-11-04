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
import com.tunjid.me.core.utilities.UriConverter
import com.tunjid.me.core.utilities.fromBytes
import com.tunjid.me.scaffold.globalui.ActualGlobalUiMutator
import com.tunjid.me.scaffold.globalui.GlobalUiMutator
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.lifecycle.ActualLifecycleMutator
import com.tunjid.me.scaffold.lifecycle.Lifecycle
import com.tunjid.me.scaffold.lifecycle.LifecycleMutator
import com.tunjid.me.scaffold.nav.*
import com.tunjid.me.scaffold.permissions.Permission
import com.tunjid.me.scaffold.permissions.Permissions
import com.tunjid.me.scaffold.permissions.PermissionsProvider
import com.tunjid.me.scaffold.savedstate.DataStoreSavedStateRepository
import com.tunjid.me.scaffold.savedstate.SavedStateRepository
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.asNoOpStateFlowMutator
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.UrlRouteMatcher
import com.tunjid.treenav.strings.routeParserFrom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import okio.Path

interface ScreenStateHolderCreator : (CoroutineScope, AppRoute) -> Any

inline fun <reified Route: AppRoute> ((CoroutineScope, Route) -> Any).downcast(): ScreenStateHolderCreator = object : ScreenStateHolderCreator{
    override fun invoke(scope: CoroutineScope, route: AppRoute): Any = this@downcast(scope, route as Route)
}

data class SavedStateType(
    val apply: PolymorphicModuleBuilder<ByteSerializable>.() -> Unit
)

class ScaffoldModule(
    val appScope: CoroutineScope,
    val savedStatePath: Path,
    val routeMatchers: List<UrlRouteMatcher<AppRoute>>,
    val permissionsProvider: PermissionsProvider,
    val byteSerializer: ByteSerializer,
    val uriConverter: UriConverter
) {
    val routeParser = routeParserFrom(*routeMatchers.toTypedArray())
    val navMutator: NavMutator = EmptyNavState.asNoOpStateFlowMutator()
    val globalUiMutator = ActualGlobalUiMutator(
        appScope = appScope,
    )
    val lifecycleMutator = ActualLifecycleMutator(
        appScope = appScope
    )
    val permissionsMutator = permissionsProvider.mutator
}

class ScaffoldComponent(
    module: ScaffoldModule
) {
    val globalUiMutator = module.globalUiMutator

    val byteSerializer = module.byteSerializer

    val uiActions = globalUiMutator.accept
}

inline fun <reified T : ByteSerializable> ScaffoldComponent.restoredState(route: AppRoute): T? {
    return try {
        // TODO: Figure out why this throws
//        val serialized = savedStateRepository.savedState.value.routeStates[route.id]
//        serialized?.let(byteSerializer::fromBytes)
        null
    } catch (e: Exception) {
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
    fun routeParser(): RouteParser<AppRoute> = routeParserFrom(*(module.routeMatchers).toTypedArray())

    @SingletonScope
    @Provides
    fun permissionsStateStream(
    ): StateFlow<Permissions> = module.permissionsProvider.mutator.state

    @SingletonScope
    @Provides
    fun permissionsActions(
    ): (Permission) -> Unit = module.permissionsProvider.mutator.accept

    @SingletonScope
    @Provides
    fun navStateStream(
        navMutator: NavMutator
    ): StateFlow<NavState> = navMutator.state

    @Provides
    fun navActions(): (NavMutation) -> Unit = navMutator.accept

    @SingletonScope
    @Provides
    fun globalUiStateStream(
        globalUiMutator: GlobalUiMutator
    ): StateFlow<UiState> = globalUiMutator.state

    @SingletonScope
    @Provides
    fun globalUiActions(
        globalUiMutator: GlobalUiMutator
    ): (Mutation<UiState>) -> Unit = globalUiMutator.accept

    @SingletonScope
    @Provides
    fun lifecycleStateStream(
        lifecycleMutator: LifecycleMutator
    ): StateFlow<Lifecycle> = lifecycleMutator.state

    val PersistedNavMutator.bind: NavMutator
        @SingletonScope
        @Provides get() = this

    val ActualGlobalUiMutator.bind: GlobalUiMutator
        @SingletonScope
        @Provides get() = this

    val ActualLifecycleMutator.bind: LifecycleMutator
        @SingletonScope
        @Provides get() = this

    val DataStoreSavedStateRepository.bind: SavedStateRepository
        @SingletonScope
        @Provides get() = this

    abstract val navMutator: NavMutator

//    abstract val globalUiMutator: GlobalUiMutator
//
//    abstract val navStateStream: StateFlow<NavState>

//    abstract val globalUiStateStream: StateFlow<UiState>
//
//    abstract val lifecycleStateStream: StateFlow<Lifecycle>

//    abstract val byteSerializer: ByteSerializer
//
//    abstract val savedStateRepository: SavedStateRepository
}