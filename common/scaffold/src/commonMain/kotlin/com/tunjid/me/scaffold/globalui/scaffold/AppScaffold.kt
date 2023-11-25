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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.LocalGlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.PaneAnchor
import com.tunjid.me.scaffold.adaptive.Adaptive
import com.tunjid.me.scaffold.adaptive.SavedStateAdaptiveContentHost
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.nav.NavStateHolder
import com.tunjid.treenav.strings.RouteParser

/**
 * Root scaffold for the app
 */
@Composable
fun Scaffold(
    modifier: Modifier,
    routeParser: RouteParser<AppRoute>,
    navStateHolder: NavStateHolder,
    globalUiStateHolder: GlobalUiStateHolder,
) {
    CompositionLocalProvider(
        LocalGlobalUiStateHolder provides globalUiStateHolder,
    ) {
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
                SavedStateAdaptiveContentHost(
                    routeParser = routeParser,
                    navState = navStateHolder.state,
                    uiState = globalUiStateHolder.state
                ) {
                    AppRouteContainer(
                        state = adaptedState,
                        onPaneAnchorChanged = remember {
                            { paneAnchor: PaneAnchor ->
                                globalUiStateHolder.accept {
                                    copy(paneAnchor = paneAnchor)
                                }
                            }
                        },
                        primaryContent = {
                            routeIn(Adaptive.Container.Primary).invoke()
                        },
                        secondaryContent = {
                            routeIn(Adaptive.Container.Secondary).invoke()
                        },
                        transientPrimaryContent = {
                            routeIn(Adaptive.Container.TransientPrimary).invoke()
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
 * Modifier for content when it is being previewed
 */
internal expect fun Modifier.backPreviewModifier(): Modifier

/**
 * Modifier for content's background when it is being previewed
 */
expect fun Modifier.backPreviewBackgroundModifier(): Modifier

