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

import com.tunjid.me.common.data.ByteSerializable
import com.tunjid.me.common.data.fromBytes
import com.tunjid.me.common.data.toBytes
import com.tunjid.me.common.nav.AppRoute
import com.tunjid.me.common.nav.ByteSerializableNav
import com.tunjid.me.common.nav.toByteSerializable
import com.tunjid.me.common.nav.toMultiStackNav
import com.tunjid.mutator.Mutator
import com.tunjid.treenav.Order
import com.tunjid.treenav.flatten
import kotlinx.coroutines.flow.StateFlow
import com.tunjid.mutator.accept
import kotlinx.serialization.Serializable

@Serializable
class SavedState(
    val navigation: ByteArray,
    val routeStates: Map<String, ByteArray>
): ByteSerializable

private const val NavKey = "com.tunjid.me.nav"

fun AppDependencies.saveState(): SavedState {
    val multiStackNav = appMutator.navMutator.state.value

    return SavedState(
        navigation = byteSerializer.toBytes(multiStackNav.toByteSerializable),
        routeStates = multiStackNav
            .flatten(order = Order.BreadthFirst)
            .filterIsInstance<AppRoute<*>>()
            .fold(mutableMapOf()) { map, route ->
                val mutator = routeDependencies(route)
                val state = (mutator as? Mutator<*, *>)?.state ?: return@fold map
                val serializable = (state as? StateFlow<*>)?.value ?: return@fold map
                if (serializable is ByteSerializable) map[route.id] =
                    byteSerializer.toBytes(serializable)
                map
            }
    )
}

fun AppDependencies.restore(savedState: SavedState) {
    val serializedNav: ByteSerializableNav = byteSerializer.fromBytes(savedState.navigation)
    val multiStackNav = serializedNav.toMultiStackNav

    appMutator.navMutator.accept {
        multiStackNav
    }
    appMutator.accept(
        AppAction.RestoreSerializedStates(
            routeIdsToSerializedStates = savedState.routeStates
        )
    )
}
