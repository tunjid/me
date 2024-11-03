package com.tunjid.scaffold.scaffold

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.tunjid.composables.splitlayout.SplitLayout
import com.tunjid.composables.splitlayout.SplitLayoutState
import com.tunjid.me.scaffold.globalui.slices.bottomNavPositionalState
import com.tunjid.me.scaffold.globalui.slices.fabState
import com.tunjid.me.scaffold.globalui.slices.snackbarPositionalState
import com.tunjid.me.scaffold.globalui.slices.uiChromeState
import com.tunjid.scaffold.scaffold.PaneAnchorState.Companion.DraggableThumb
import com.tunjid.scaffold.scaffold.PaneAnchorState.Companion.MinPaneWidth
import com.tunjid.me.scaffold.scaffold.configuration.predictiveBackConfiguration
import com.tunjid.me.scaffold.scaffold.restrictedSizePlacement
import com.tunjid.me.scaffold.scaffold.routePanePadding
import com.tunjid.treenav.compose.PaneState
import com.tunjid.treenav.compose.PanedNavHost
import com.tunjid.treenav.compose.configurations.animatePaneBoundsConfiguration
import com.tunjid.treenav.compose.configurations.paneModifierConfiguration
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementHostState
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.configurations.canAnimateOnStartingFrames
import com.tunjid.treenav.compose.threepane.configurations.threePanedMovableSharedElementConfiguration
import com.tunjid.treenav.compose.threepane.configurations.threePanedNavHostConfiguration
import com.tunjid.treenav.strings.Route

/**
 * Root scaffold for the app
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MeApp(
    modifier: Modifier,
    meAppState: MeAppState,
) {
    val paneRenderOrder = remember {
        listOf(
            ThreePane.Secondary,
            ThreePane.Primary,
        )
    }
    val splitLayoutState = remember {
        SplitLayoutState(
            orientation = Orientation.Horizontal,
            maxCount = paneRenderOrder.size,
            minSize = MinPaneWidth,
            keyAtIndex = { index ->
                val indexDiff = paneRenderOrder.size - visibleCount
                paneRenderOrder[index + indexDiff]
            }
        )
    }
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalAppState provides meAppState,
    ) {
        Surface {
            Box(
                modifier = modifier.fillMaxSize()
            ) {
                AppNavRail(
                    navItems = meAppState.navItems,
                    uiChromeState = remember {
                        derivedStateOf { meAppState.globalUi.uiChromeState }
                    }.value,
                    onNavItemSelected = meAppState::onNavItemSelected,
                )
                // Root LookaheadScope used to anchor all shared element transitions
                SharedTransitionScope { sharedElementModifier ->
                    val movableSharedElementHostState = remember {
                        MovableSharedElementHostState(
                            sharedTransitionScope = this@SharedTransitionScope,
                            canAnimateOnStartingFrames = PaneState<ThreePane, Route>::canAnimateOnStartingFrames
                        )
                    }
                    PanedNavHost(
                        modifier = Modifier.fillMaxSize(),
                        state = meAppState.rememberPanedNavHostState {
                            this
                                .threePanedNavHostConfiguration(
                                    windowWidthState = derivedStateOf {
                                        splitLayoutState.size
                                    }
                                )
                                .predictiveBackConfiguration(
                                    windowSizeClassState = derivedStateOf {
                                        meAppState.globalUi.windowSizeClass
                                    },
                                    backStatusState = derivedStateOf {
                                        meAppState.globalUi.backStatus
                                    },
                                )
                                .threePanedMovableSharedElementConfiguration(
                                    movableSharedElementHostState
                                )
                                .paneModifierConfiguration {
                                    Modifier.restrictedSizePlacement(
                                        atStart = paneState.pane == ThreePane.Secondary
                                    )
                                }
                                .animatePaneBoundsConfiguration(
                                    lookaheadScope = this@SharedTransitionScope,
                                    shouldAnimatePane = {
                                        when (paneState.pane) {
                                            ThreePane.Primary,
                                            ThreePane.Secondary,
                                            ThreePane.Tertiary -> !meAppState.paneAnchorState.hasInteractions

                                            ThreePane.TransientPrimary -> true
                                            ThreePane.Overlay,
                                            null -> false
                                        }
                                    }
                                )
                        },
                    ) {
                        val filteredOrder by remember {
                            derivedStateOf { paneRenderOrder.filter { nodeFor(it) != null } }
                        }
                        splitLayoutState.visibleCount = filteredOrder.size
                        meAppState.paneAnchorState.updateMaxWidth(
                            with(density) { splitLayoutState.size.roundToPx() }
                        )
                        SplitLayout(
                            state = splitLayoutState,
                            modifier = modifier
                                .fillMaxSize()
                                .then(sharedElementModifier)
                                .then(movableSharedElementHostState.modifier)
                                .routePanePadding(
                                    state = remember {
                                        derivedStateOf { meAppState.globalUi.uiChromeState }
                                    }
                                ),
                            itemSeparators = { _, offset ->
                                DraggableThumb(
                                    splitLayoutState = splitLayoutState,
                                    paneAnchorState = meAppState.paneAnchorState,
                                    offset = offset
                                )
                            },
                            itemContent = { index ->
                                DragToPopLayout(
                                    state = meAppState,
                                    pane = filteredOrder[index]
                                )
                            }
                        )
                        LaunchedEffect(meAppState.paneAnchorState.currentPaneAnchor) {
                            meAppState.updateGlobalUi {
                                copy(paneAnchor = meAppState.paneAnchorState.currentPaneAnchor)
                            }
                        }
                        LaunchedEffect(filteredOrder) {
                            if (filteredOrder.size != 1) return@LaunchedEffect
                            meAppState.paneAnchorState.onClosed()
                        }
                    }
                }
                AppFab(
                    state = remember {
                        derivedStateOf { meAppState.globalUi.fabState }
                    }.value,
                    onClicked = {
                        meAppState.globalUi.fabClickListener(Unit)
                    }
                )
                AppBottomNav(
                    navItems = meAppState.navItems,
                    positionalState = remember {
                        derivedStateOf { meAppState.globalUi.bottomNavPositionalState }
                    }.value,
                    onNavItemSelected = meAppState::onNavItemSelected,
                )
                AppSnackBar(
                    state = remember {
                        derivedStateOf { meAppState.globalUi.snackbarPositionalState }
                    }.value,
                    queue = remember {
                        derivedStateOf { meAppState.globalUi.snackbarMessages }
                    }.value,
                    onMessageClicked = { message ->
                        meAppState.globalUi.snackbarMessageConsumer(message)
                    },
                    onSnackbarOffsetChanged = { offset ->
                        meAppState.updateGlobalUi { copy(snackbarOffset = offset) }
                    },
                )
            }
        }
    }
}
