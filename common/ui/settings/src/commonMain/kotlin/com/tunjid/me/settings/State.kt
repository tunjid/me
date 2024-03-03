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

package com.tunjid.me.settings

import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.scaffold.navigation.NavigationAction
import com.tunjid.me.scaffold.navigation.NavigationMutation
import com.tunjid.scaffold.adaptive.ExternalRoute
import com.tunjid.treenav.push
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    @Transient
    val routes: List<ExternalRoute> = listOf(
        ExternalRoute(path = "/sign-in")
    )
) : ByteSerializable

sealed class Action(val key: String) {
    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data class External(
            val externalRoute: ExternalRoute
        ) : Navigate() {
            override val navigationMutation: NavigationMutation = {
                navState.push(
                    externalRoute.path.toRoute
                )
            }
        }
    }
}