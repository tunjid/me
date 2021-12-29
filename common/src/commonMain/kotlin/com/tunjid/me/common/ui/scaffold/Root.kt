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

package com.tunjid.me.common.ui.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.tunjid.me.common.AppDeps
import com.tunjid.me.common.LocalAppDependencies
import com.tunjid.me.common.nav.MultiStackNav
import com.tunjid.me.common.nav.Route404
import com.tunjid.me.common.nav.current
import com.tunjid.me.common.ui.mappedCollectAsState

@Composable
fun Root(appDeps: AppDeps) {
    CompositionLocalProvider(LocalAppDependencies provides appDeps) {
        val globalUiMutator = LocalAppDependencies.current.globalUiMutator
        val navMutator = LocalAppDependencies.current.navMutator

        val route by navMutator.state.mappedCollectAsState(mapper = MultiStackNav::current)

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AppNavRail(
                globalUiMutator = globalUiMutator,
                navMutator = navMutator,
            )
            AppToolbar(
                globalUiMutator = globalUiMutator,
                navMutator = navMutator,
            )
            AppRouteContainer(
                globalUiMutator = globalUiMutator,
                navMutator = navMutator,
                content = {
                    (route ?: Route404).Render()
                }
            )
            AppBottomNav(
                globalUiMutator = globalUiMutator,
                navMutator = navMutator,
            )
        }
    }
}
