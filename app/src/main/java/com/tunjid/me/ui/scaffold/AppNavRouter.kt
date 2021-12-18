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

package com.tunjid.me.ui.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.tunjid.me.nav.MultiStackNav
import com.tunjid.me.nav.Route
import com.tunjid.me.nav.current
import com.tunjid.me.ui.mapState
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun AppNavRouter(
    navStateFlow: StateFlow<MultiStackNav>
) {
    val scope = rememberCoroutineScope()
    val routeState = navStateFlow
        .mapState(scope, MultiStackNav::current)
        .collectAsState()

    when (val route = routeState.value) {
        is Route -> route.Render()
        else -> Box {
            Text(
                modifier = Modifier
                    .padding(),
                text = "404"
            )
        }
    }
}
