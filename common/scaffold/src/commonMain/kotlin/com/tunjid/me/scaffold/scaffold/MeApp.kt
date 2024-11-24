package com.tunjid.me.scaffold.scaffold

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tunjid.composables.backpreview.backPreview
import com.tunjid.composables.constrainedsize.constrainedSizePlacement
import com.tunjid.composables.splitlayout.SplitLayout
import com.tunjid.composables.splitlayout.SplitLayoutState
import com.tunjid.me.scaffold.globalui.slices.bottomNavPositionalState
import com.tunjid.me.scaffold.globalui.slices.fabState
import com.tunjid.me.scaffold.globalui.slices.snackbarPositionalState
import com.tunjid.me.scaffold.globalui.slices.uiChromeState
import com.tunjid.me.scaffold.scaffold.PaneAnchorState.Companion.DraggableThumb
import com.tunjid.me.scaffold.scaffold.PaneAnchorState.Companion.MinPaneWidth
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.compose.PanedNavHost
import com.tunjid.treenav.compose.configurations.animatePaneBoundsConfiguration
import com.tunjid.treenav.compose.configurations.paneModifierConfiguration
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementHostState
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.configurations.predictiveBackConfiguration
import com.tunjid.treenav.compose.threepane.configurations.threePanedMovableSharedElementConfiguration
import com.tunjid.treenav.compose.threepane.configurations.threePanedNavHostConfiguration
import com.tunjid.treenav.pop
import com.tunjid.treenav.strings.Route

/**
 * Root scaffold for the app
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MeApp(
    modifier: Modifier,
    appState: AppState,
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
                    PanedNavHost(
                        modifier = Modifier.fillMaxSize(),
                        state = appState.rememberPanedNavHostState {
                            this
                                .paneModifierConfiguration {
                                    Modifier
                                        .fillMaxSize()
                                        .constrainedSizePlacement(
                                            orientation = Orientation.Horizontal,
                                            minSize = 180.dp,
                                            atStart = paneState.pane == ThreePane.Secondary,
                                        )
                                        .padding(
                                            horizontal = if (splitLayoutState.visibleCount > 1) 16.dp else 0.dp
                                        )
                                        .run {
                                            if (paneState.pane == ThreePane.TransientPrimary) backPreview(
                                                appState.backPreviewState
                                            )
                                            else this
                                        }
                                }
                                .threePanedNavHostConfiguration(
                                    windowWidthState = derivedStateOf {
                                        splitLayoutState.size
                                    }
                                )
                                .predictiveBackConfiguration(
                                    isPreviewingBack = derivedStateOf {
                                        appState.isPreviewingBack
                                    },
                                    backPreviewTransform = MultiStackNav::pop,
                                )
                                .threePanedMovableSharedElementConfiguration(
                                    movableSharedElementHostState
                                )
                                .animatePaneBoundsConfiguration(
                                    lookaheadScope = this@SharedTransitionScope,
                                    shouldAnimatePane = {
                                        when (paneState.pane) {
                                            ThreePane.Primary,
                                            ThreePane.Secondary,
                                            ThreePane.Tertiary -> !appState.paneAnchorState.hasInteractions

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
                        appState.paneAnchorState.updateMaxWidth(
                            with(density) { splitLayoutState.size.roundToPx() }
                        )
                        SplitLayout(
                            state = splitLayoutState,
                            modifier = modifier
                                .fillMaxSize()
                                .then(sharedElementModifier)
                                .routePanePadding(
                                    state = remember {
                                        derivedStateOf { appState.globalUi.uiChromeState }
                                    }
                                ),
                            itemSeparators = { _, offset ->
                                DraggableThumb(
                                    splitLayoutState = splitLayoutState,
                                    paneAnchorState = appState.paneAnchorState,
                                    offset = offset
                                )
                            },
                            itemContent = { index ->
                                DragToPopLayout(
                                    state = appState,
                                    pane = filteredOrder[index]
                                )
                            }
                        )
                        LaunchedEffect(appState.paneAnchorState.currentPaneAnchor) {
                            appState.updateGlobalUi {
                                copy(paneAnchor = appState.paneAnchorState.currentPaneAnchor)
                            }
                        }
                        LaunchedEffect(filteredOrder) {
                            if (filteredOrder.size != 1) return@LaunchedEffect
                            appState.paneAnchorState.onClosed()
                        }
                    }
                }
                AppFab(
                    state = remember {
                        derivedStateOf { appState.globalUi.fabState }
                    }.value,
                    onClicked = {
                        appState.globalUi.fabClickListener(Unit)
                    }
                )
                AppBottomNav(
                    navItems = appState.navItems,
                    positionalState = remember {
                        derivedStateOf { appState.globalUi.bottomNavPositionalState }
                    }.value,
                    onNavItemSelected = appState::onNavItemSelected,
                )
                AppNavRail(
                    navItems = appState.navItems,
                    uiChromeState = remember {
                        derivedStateOf { appState.globalUi.uiChromeState }
                    }.value,
                    onNavItemSelected = appState::onNavItemSelected,
                )
                AppSnackBar(
                    state = remember {
                        derivedStateOf { appState.globalUi.snackbarPositionalState }
                    }.value,
                    queue = remember {
                        derivedStateOf { appState.globalUi.snackbarMessages }
                    }.value,
                    onMessageClicked = { message ->
                        appState.globalUi.snackbarMessageConsumer(message)
                    },
                    onSnackbarOffsetChanged = { offset ->
                        appState.updateGlobalUi { copy(snackbarOffset = offset) }
                    },
                )
            }
        }
    }
}
