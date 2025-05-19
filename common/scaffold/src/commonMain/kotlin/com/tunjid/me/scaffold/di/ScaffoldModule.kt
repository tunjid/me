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

import androidx.lifecycle.ViewModel
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.core.utilities.fromBytes
import com.tunjid.me.data.di.InjectedDataComponent
import com.tunjid.me.scaffold.navigation.NavigationMutation
import com.tunjid.me.scaffold.navigation.NavigationStateHolder
import com.tunjid.me.scaffold.navigation.PersistedNavigationStateHolder
import com.tunjid.me.scaffold.permissions.Permission
import com.tunjid.me.scaffold.permissions.Permissions
import com.tunjid.me.scaffold.permissions.PermissionsProvider
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.routeParserFrom
import com.tunjid.treenav.strings.urlRouteMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class ScaffoldScope

interface ScreenStateHolderCreator : (CoroutineScope, Route) -> ViewModel

typealias SavedStateCache = (Route) -> ByteArray?

fun ((CoroutineScope, Route) -> ViewModel).downcast(): ScreenStateHolderCreator =
    object : ScreenStateHolderCreator {
        override fun invoke(scope: CoroutineScope, route: Route): ViewModel =
            this@downcast(
                scope,
                route,
            )
    }


fun <T : Route> routeAndMatcher(
    routePattern: String,
    routeMapper: (RouteParams) -> T,
) = routePattern to urlRouteMatcher(
    routePattern = routePattern,
    routeMapper = routeMapper
)

/**
 * Wrapper for [ByteSerializer] to get around ksp compilation issues
 */
class ByteSerializerWrapper(byteSerializer: ByteSerializer) : ByteSerializer by byteSerializer

data class SavedStateType(
    val apply: PolymorphicModuleBuilder<ByteSerializable>.() -> Unit,
)

class ScaffoldModule(
    val appScope: CoroutineScope,
    val routeMatchers: List<RouteMatcher>,
    val permissionsProvider: PermissionsProvider,
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

@ScaffoldScope
@Component
abstract class InjectedScaffoldComponent(
    private val module: ScaffoldModule,
    @Component
    val dataComponent: InjectedDataComponent,
) {
    @ScaffoldScope
    @Provides
    fun appScope(): CoroutineScope = module.appScope

    @ScaffoldScope
    @Provides
    fun routeParser(): RouteParser =
        routeParserFrom(*(module.routeMatchers).toTypedArray())

    @ScaffoldScope
    @Provides
    fun permissionsStateStream(
    ): StateFlow<Permissions> = module.permissionsProvider.stateHolder.state

    @ScaffoldScope
    @Provides
    fun permissionsActions(
    ): (Permission) -> Unit = module.permissionsProvider.stateHolder.accept

    @ScaffoldScope
    @Provides
    fun navStateStream(
        navStateHolder: NavigationStateHolder,
    ): StateFlow<MultiStackNav> = navStateHolder.state

    @Provides
    fun navActions(): (NavigationMutation) -> Unit = navStateHolder.accept

    val PersistedNavigationStateHolder.bind: NavigationStateHolder
        @ScaffoldScope
        @Provides get() = this

    abstract val navStateHolder: NavigationStateHolder
}