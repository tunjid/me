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

package com.tunjid.me.scaffold.nav

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.tunjid.mutator.accept
import com.tunjid.mutator.coroutines.asNoOpStateFlowMutator
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.Route

class Navigator(
    private val navMutator: NavMutator,
    private val patternsToParsers: Map<Regex, RouteParser<*>>
) {
    val currentNav get() = navMutator.state.value
    val String.toRoute: Route
        get() = patternsToParsers.parse(this)

    fun navigate(action: Navigator.() -> MultiStackNav) {
        navMutator.accept {
            val changedNav = action(this@Navigator)
            changedNav
        }
    }
}

val LocalNavigator: ProvidableCompositionLocal<Navigator> = staticCompositionLocalOf {
    Navigator(
        navMutator = MultiStackNav("AppNav").asNoOpStateFlowMutator(),
        patternsToParsers = mapOf(),
    )
}

internal fun Map<Regex, RouteParser<*>>.parse(route: String): Route {
    val regex = keys.firstOrNull { it.matches(input = route) } ?: return Route404
    return when (val result = regex.matchEntire(input = route)) {
        null -> Route404
        else -> {
            val routeParser = getValue(regex)
            routeParser.route(
                RouteParams(
                    route = route,
                    pathArgs = routeParser.pathKeys
                        .zip(result.groupValues.drop(1))
                        .toMap(),
                    // TODO: Parse query parameters
                    queryArgs = mapOf(),
                )
            )
        }
    }
}

internal fun List<RouteParser<*>>.patternsToParsers(): Map<Regex, RouteParser<*>> =
    fold(mapOf()) { map, parser ->
        map + parser.patterns.map { Regex(it) to parser }
    }