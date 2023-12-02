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

package com.tunjid.me.scaffold.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.tunjid.treenav.strings.RouteParams
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

typealias SerializedRouteParams = @Serializable(RouteParamsSerializer::class) RouteParams

@Serializable
@SerialName("RouteParams")
private class RouteParamsSurrogate(
    val route: String,
    val pathArgs: Map<String, String>,
    val queryParams: Map<String, List<String>>,
)

private object RouteParamsSerializer : KSerializer<RouteParams> {
    override val descriptor: SerialDescriptor = RouteParamsSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: RouteParams) {
        val surrogate = RouteParamsSurrogate(
            route = value.route,
            pathArgs = value.pathArgs,
            queryParams = value.queryParams
        )
        encoder.encodeSerializableValue(RouteParamsSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): RouteParams {
        val surrogate = decoder.decodeSerializableValue(RouteParamsSurrogate.serializer())
        return RouteParams(
            route = surrogate.route,
            pathArgs = surrogate.pathArgs,
            queryParams = surrogate.queryParams
        )
    }
}

@Serializable
data class UnknownRoute(val path: String = "404") : AppRoute, StatelessRoute {
    override val routeParams: RouteParams get() = RouteParams(
        route = path,
        pathArgs = emptyMap(),
        queryParams = emptyMap()
    )

    @Composable
    override fun content() {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(),
                text = "404",
                fontSize = 40.sp
            )
        }
    }
}
