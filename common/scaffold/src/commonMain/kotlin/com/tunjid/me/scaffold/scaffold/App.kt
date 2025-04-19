/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.me.scaffold.scaffold

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tunjid.composables.backpreview.backPreview
import com.tunjid.composables.constrainedsize.constrainedSizePlacement
import com.tunjid.composables.splitlayout.SplitLayout
import com.tunjid.me.scaffold.scaffold.PaneAnchorState.Companion.DraggableThumb
import com.tunjid.me.scaffold.ui.theme.AppTheme
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.compose.MultiPaneDisplay
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementHostState
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.transforms.backPreviewTransform
import com.tunjid.treenav.compose.threepane.transforms.threePanedAdaptiveTransform
import com.tunjid.treenav.compose.threepane.transforms.threePanedMovableSharedElementTransform
import com.tunjid.treenav.compose.transforms.paneModifierTransform
import com.tunjid.treenav.pop
import com.tunjid.treenav.strings.Route

/**
 * Root scaffold for the app
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun App(
    modifier: Modifier,
    appState: AppState,
) {
    AppTheme {
        CompositionLocalProvider(
            LocalAppState provides appState,
        ) {
            Surface {
                Box(
                    modifier = modifier.fillMaxSize()
                ) {
                    // Root LookaheadScope used to anchor all shared element transitions
                    SharedTransitionScope { sharedElementModifier ->
                        val movableSharedElementHostState = remember {
                            MovableSharedElementHostState<ThreePane, Route>(
                                sharedTransitionScope = this@SharedTransitionScope,
                            )
                        }
                        MultiPaneDisplay(
                            modifier = Modifier.fillMaxSize(),
                            state = appState.rememberMultiPaneDisplayState(
                                transforms = listOf(
                                    threePanedAdaptiveTransform(
                                        secondaryPaneBreakPoint = mutableStateOf(
                                            SecondaryPaneMinWidthBreakpointDp
                                        ),
                                        tertiaryPaneBreakPoint = mutableStateOf(
                                            TertiaryPaneMinWidthBreakpointDp
                                        ),
                                        windowWidthState = derivedStateOf {
                                            appState.splitLayoutState.size
                                        }
                                    ),
                                    backPreviewTransform(
                                        isPreviewingBack = derivedStateOf {
                                            appState.isPreviewingBack
                                        },
                                        navigationStateBackTransform = MultiStackNav::pop,
                                    ),
                                    threePanedMovableSharedElementTransform(
                                        movableSharedElementHostState
                                    ),
                                    paneModifierTransform {
                                        Modifier
                                            .fillMaxSize()
                                            .constrainedSizePlacement(
                                                orientation = Orientation.Horizontal,
                                                minSize = 180.dp,
                                                atStart = paneState.pane == ThreePane.Secondary,
                                            )
                                            .run {
                                                if (paneState.pane == ThreePane.TransientPrimary) backPreview(
                                                    appState.backPreviewState
                                                )
                                                else this
                                            }
                                    },
                                )
                            ),
                        ) {
                            appState.displayScope = this
                            appState.splitLayoutState.visibleCount = appState.filteredPaneOrder.size
                            appState.paneAnchorState.updateMaxWidth(
                                with(LocalDensity.current) { appState.splitLayoutState.size.roundToPx() }
                            )
                            SplitLayout(
                                state = appState.splitLayoutState,
                                modifier = modifier
                                    .fillMaxSize()
                                    .then(sharedElementModifier),
                                itemSeparators = { _, offset ->
                                    DraggableThumb(
                                        splitLayoutState = appState.splitLayoutState,
                                        paneAnchorState = appState.paneAnchorState,
                                        offset = offset
                                    )
                                },
                                itemContent = { index ->
                                    DragToPopLayout(
                                        state = appState,
                                        pane = appState.filteredPaneOrder[index]
                                    )
                                }
                            )
                            LaunchedEffect(Unit) {
                                snapshotFlow { appState.filteredPaneOrder }.collect { order ->
                                    if (order.size != 1) return@collect
                                    appState.paneAnchorState.onClosed()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}