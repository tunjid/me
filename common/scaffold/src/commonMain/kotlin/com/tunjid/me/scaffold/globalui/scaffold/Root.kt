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

package com.tunjid.me.scaffold.globalui.scaffold

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.tunjid.me.core.utilities.mappedCollectAsState
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.LocalGlobalUiStateHolder
import com.tunjid.me.scaffold.lifecycle.mappedCollectAsStateWithLifecycle
import com.tunjid.me.scaffold.nav.*

/**
 * Root scaffold for the app
 */
@Composable
fun Scaffold(
    modifier: Modifier,
    navStateHolder: NavStateHolder,
    globalUiStateHolder: GlobalUiStateHolder,
) {
    CompositionLocalProvider(
        LocalGlobalUiStateHolder provides globalUiStateHolder,
    ) {
        val saveableStateHolder = rememberSaveableStateHolder()

        val route by navStateHolder.state.mappedCollectAsStateWithLifecycle(mapper = NavState::current)
        val renderedRoute = route as? AppRoute ?: Route404

        Box(
            modifier = modifier.fillMaxSize()
        ) {
            AppNavRail(
                globalUiStateHolder = globalUiStateHolder,
                navStateHolder = navStateHolder,
                saveableStateHolder = saveableStateHolder,
            )
            AppToolbar(
                globalUiStateHolder = globalUiStateHolder,
                navStateHolder = navStateHolder,
            )
            saveableStateHolder.SaveableStateProvider(renderedRoute.id) {
                AppRouteContainer(
                    globalUiStateHolder = globalUiStateHolder,
                    navStateHolder = navStateHolder,
                    content = {
                        Crossfade(
                            targetState = renderedRoute,
                            content = { it.Render() }
                        )
                    }
                )
            }
            AppFab(
                globalUiStateHolder = globalUiStateHolder,
            )
            AppBottomNav(
                globalUiStateHolder = globalUiStateHolder,
                navStateHolder = navStateHolder,
            )
            AppSnackBar(
                globalUiStateHolder = globalUiStateHolder,
            )
        }
    }
}
