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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.LocalGlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.adaptive.AdaptiveContentHost
import com.tunjid.me.scaffold.globalui.adaptive.Adaptive
import com.tunjid.me.scaffold.nav.NavStateHolder
import com.tunjid.me.scaffold.globalui.adaptive.adaptiveNavigationState
import com.tunjid.me.scaffold.globalui.adaptive.slotFor

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
        val uiStateFlow = remember {
            globalUiStateHolder.state
        }
        val adaptiveNavigationStateFlow = remember {
            navStateHolder.state.adaptiveNavigationState(uiStateFlow)
        }
        val adaptiveNavigationState by adaptiveNavigationStateFlow.collectAsState(
            Adaptive.NavigationState.Initial
        )
        val adaptation by remember {
            derivedStateOf { adaptiveNavigationState.adaptation }
        }
        Surface {
            Box(
                modifier = modifier.fillMaxSize()
            ) {
                AppNavRail(
                    globalUiStateHolder = globalUiStateHolder,
                    navStateHolder = navStateHolder,
                )
                AppToolbar(
                    globalUiStateHolder = globalUiStateHolder,
                    navStateHolder = navStateHolder,
                )
                AdaptiveContentHost(
                    navStateHolder = navStateHolder,
                    adaptiveNavigationState = adaptiveNavigationState
                ) {
                    AppRouteContainer(
                        globalUiStateHolder = globalUiStateHolder,
                        navStateHolder = navStateHolder,
                        adaptation = adaptation,
                        primaryContent = {
                            routeIn(
                                adaptiveNavigationState.slotFor(Adaptive.Container.Primary)
                            ).invoke()
                        },
                        secondaryContent = {
                            routeIn(
                                adaptiveNavigationState.slotFor(Adaptive.Container.Secondary)
                            ).invoke()
                        },
                        transientPrimaryContent = {
                            routeIn(
                                adaptiveNavigationState.slotFor(Adaptive.Container.TransientPrimary)
                            ).invoke()
                        },
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
}

/**
 * Modifier that offers a way to preview content behind the primary content
 */
internal expect fun Modifier.backPreviewModifier(): Modifier
