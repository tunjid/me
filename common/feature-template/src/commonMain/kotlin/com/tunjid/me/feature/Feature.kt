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

package com.tunjid.me.feature

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.tunjid.me.data.di.DataComponent
import com.tunjid.me.scaffold.di.ScaffoldComponent
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.nav.RouteParser
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass

const val FeatureWhileSubscribed = 2_000L

interface Feature<Route : AppRoute, Mutator: Any> {
    val routeType: KClass<Route>
    val routeParsers: List<RouteParser<Route>>
    fun mutator(
        scope: CoroutineScope,
        route: Route,
        scaffoldComponent: ScaffoldComponent,
        dataComponent: DataComponent
    ): Mutator
}

interface RouteServiceLocator {
    fun <T> locate(route: AppRoute): T
}

val LocalRouteServiceLocator: ProvidableCompositionLocal<RouteServiceLocator> = staticCompositionLocalOf {
    object : RouteServiceLocator {
        override fun <T> locate(route: AppRoute): T {
            TODO("Not yet implemented")
        }
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified Route: AppRoute> List<Feature<*, *>>.find(route: Route): Feature<Route, *> =
    first { feature ->
        feature.routeType.isInstance(route)
    } as Feature<Route, *>