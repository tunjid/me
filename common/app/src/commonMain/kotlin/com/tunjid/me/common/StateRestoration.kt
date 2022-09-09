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

package com.tunjid.me.common

import com.tunjid.me.common.di.AppDependencies
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.utilities.toBytes
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.nav.toMultiStackNav
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.mutation
import com.tunjid.treenav.Order
import com.tunjid.treenav.flatten
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
class SavedState(
    val activeNav: Int = 0,
    val navigation: List<List<String>>,
    val routeStates: Map<String, ByteArray>
) : ByteSerializable

fun AppDependencies.saveState(): SavedState {
    val navState = scaffoldComponent.navStateStream.value
    val multiStackNav = navState.rootNav
    return SavedState(
        activeNav = multiStackNav.currentIndex,
        navigation = multiStackNav.stacks.fold(listOf()) { listOfLists, stackNav ->
            listOfLists.plus(
                element = stackNav.routes
                    .filterIsInstance<AppRoute>()
                    .fold(listOf()) { stackList, route ->
                        stackList + route.id
                    }
            )
        },
        routeStates = multiStackNav.flatten(order = Order.BreadthFirst)
            .filterIsInstance<AppRoute>()
            .fold(mutableMapOf()) { map, route ->
                val mutator = routeServiceLocator.locate<Any>(route)
                val state = (mutator as? ActionStateProducer<*, *>)?.state ?: return@fold map
                val serializable = (state as? StateFlow<*>)?.value ?: return@fold map
                if (serializable is ByteSerializable) map[route.id] = byteSerializer.toBytes(serializable)
                map
            })
}

fun AppDependencies.restore(savedState: SavedState) = scaffoldComponent.apply {
    navActions {
        scaffoldComponent.routeParser.toMultiStackNav(
            savedState.navigation
        ).copy(currentIndex = savedState.activeNav)
    }
    lifecycleActions(mutation {
        copy(routeIdsToSerializedStates = savedState.routeStates)
    })
}
