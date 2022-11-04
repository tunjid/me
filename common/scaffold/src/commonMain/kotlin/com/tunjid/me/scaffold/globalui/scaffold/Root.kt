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
import com.tunjid.me.scaffold.di.InjectedScaffoldComponent
import com.tunjid.me.scaffold.di.ScaffoldComponent
import com.tunjid.me.scaffold.globalui.LocalGlobalUiMutator
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.nav.NavState
import com.tunjid.me.scaffold.nav.Route404
import com.tunjid.me.scaffold.nav.current
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.asNoOpStateFlowMutator

/**
 * Root scaffold for the app
 */
@Composable
fun Scaffold(
    modifier: Modifier,
    component: InjectedScaffoldComponent,
) {
    val navMutator = component.navMutator
    val globalUiMutator = UiState().asNoOpStateFlowMutator<Mutation<UiState>, UiState>()

    CompositionLocalProvider(
        LocalGlobalUiMutator provides globalUiMutator,
    ) {
        val saveableStateHolder = rememberSaveableStateHolder()

        val route by navMutator.state.mappedCollectAsState(mapper = NavState::current)
        val renderedRoute = route as? AppRoute ?: Route404

        Box(
            modifier = modifier.fillMaxSize()
        ) {
            AppNavRail(
                globalUiMutator = globalUiMutator,
                navMutator = navMutator,
                saveableStateHolder = saveableStateHolder,
            )
            AppToolbar(
                globalUiMutator = globalUiMutator,
                navMutator = navMutator,
            )
            saveableStateHolder.SaveableStateProvider(renderedRoute.id) {
                AppRouteContainer(
                    globalUiMutator = globalUiMutator,
                    navMutator = navMutator,
                    content = {
                        Crossfade(
                            targetState = renderedRoute,
                            content = { it.Render() }
                        )
                    }
                )
            }
            AppFab(
                globalUiMutator = globalUiMutator,
            )
            AppBottomNav(
                globalUiMutator = globalUiMutator,
                navMutator = navMutator,
            )
            AppSnackBar(
                globalUiMutator = globalUiMutator,
            )
        }
    }
}
