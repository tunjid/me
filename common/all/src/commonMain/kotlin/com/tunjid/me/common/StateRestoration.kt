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

import com.tunjid.me.scaffold.lifecycle.LifecycleAction
import com.tunjid.me.common.di.AppDependencies
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.utilities.fromBytes
import com.tunjid.me.core.utilities.toBytes
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.nav.ByteSerializableNav
import com.tunjid.me.scaffold.nav.toByteSerializable
import com.tunjid.me.scaffold.nav.toMultiStackNav
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
): ByteSerializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SavedState

        if (!navigation.contentEquals(other.navigation)) return false
        if (routeStates.keys != other.routeStates.keys) return false
        return routeStates.values.map { it } != other.routeStates.values.map { it }
    }

    override fun hashCode(): Int {
        var result = navigation.contentHashCode()
        result = 31 * result + routeStates.hashCode()
        return result
    }
}

fun AppDependencies.saveState(): SavedState {
    return SavedState(
        navigation = byteArrayOf(),
        routeStates = mapOf()
    )
//    val multiStackNav = appMutator.navMutator.state.value
//
//    return SavedState(
//        navigation = byteSerializer.toBytes(multiStackNav.toByteSerializable),
//        routeStates = multiStackNav
//            .flatten(order = Order.BreadthFirst)
//            .filterIsInstance<AppRoute<*>>()
//            .fold(mutableMapOf()) { map, route ->
//                val mutator = routeDependencies(route)
//                val state = (mutator as? Mutator<*, *>)?.state ?: return@fold map
//                val serializable = (state as? StateFlow<*>)?.value ?: return@fold map
//                if (serializable is ByteSerializable) map[route.id] =
//                    byteSerializer.toBytes(serializable)
//                map
//            }
//    )
}

fun AppDependencies.restore(savedState: SavedState) {
//    appMutator.navMutator.accept {
//        val serializedNav: ByteSerializableNav = byteSerializer.fromBytes(savedState.navigation)
//        serializedNav.toMultiStackNav
//    }
//    appMutator.accept(
//        LifecycleAction.RestoreSerializedStates(
//            routeIdsToSerializedStates = savedState.routeStates
//        )
//    )
}
