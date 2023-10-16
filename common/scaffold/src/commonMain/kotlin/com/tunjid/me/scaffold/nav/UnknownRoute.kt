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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable

@Serializable
data class UnknownRoute(override val id: String = "404") : AppRoute, StatelessRoute {

    override val content: @Composable Adaptive.ContainerScope.() -> Unit
        get() = {
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
