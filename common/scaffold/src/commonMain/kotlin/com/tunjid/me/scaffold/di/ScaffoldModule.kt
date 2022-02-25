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

import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.core.utilities.fromBytes
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.globalUiMutator
import com.tunjid.me.scaffold.lifecycle.lifecycleMutator
import com.tunjid.me.scaffold.nav.*
import kotlinx.coroutines.CoroutineScope

class ScaffoldModule(
    appScope: CoroutineScope,
    initialUiState: UiState = UiState(),
    startRoutes: List<List<String>>,
    routeParsers: List<RouteParser<*>>,
    internal val byteSerializer: ByteSerializer
) {
    internal val patternsToParsers = routeParsers.patternsToParsers()
    val navMutator = navMutator(
        scope = appScope,
        startNav = startRoutes,
        patternsToParsers = patternsToParsers,
    )
    val globalUiMutator = globalUiMutator(
        scope = appScope,
        initialState = initialUiState
    )
    val lifecycleMutator = lifecycleMutator(
        scope = appScope
    )
}

class ScaffoldComponent(
    module: ScaffoldModule
) {
    internal val navMutator = module.navMutator
    internal val globalUiMutator = module.globalUiMutator
    private val lifecycleMutator = module.lifecycleMutator

    val patternsToParsers = module.patternsToParsers
    val byteSerializer = module.byteSerializer

    val navStateStream = module.navMutator.state
    val globalUiStateStream = module.globalUiMutator.state
    val lifecycleStateStream = module.lifecycleMutator.state

    val navActions = navMutator.accept
    val uiActions = globalUiMutator.accept
    val lifecycleActions = lifecycleMutator.accept

    val navigator = Navigator(
        navMutator = navMutator,
        patternsToParsers = patternsToParsers
    )
}

inline fun <reified T : ByteSerializable> ScaffoldComponent.restoredState(route: AppRoute): T? {
    return try {
        // TODO: Figure out why this throws
        val serialized = lifecycleStateStream.value.routeIdsToSerializedStates[route.id]
        serialized?.let(byteSerializer::fromBytes)
    } catch (e: Exception) {
        null
    }
}