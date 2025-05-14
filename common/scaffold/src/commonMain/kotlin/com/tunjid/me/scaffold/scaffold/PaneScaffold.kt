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

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.zIndex
import com.tunjid.composables.ui.skipIf
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.threepane.PaneMovableElementSharedTransitionScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.rememberPaneMovableElementSharedTransitionScope
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs

class PaneScaffoldState internal constructor(
    density: Density,
    internal val appState: AppState,
    paneMovableElementSharedTransitionScope: PaneMovableElementSharedTransitionScope<Route>,
) : PaneMovableElementSharedTransitionScope<Route> by paneMovableElementSharedTransitionScope {
    val isMediumScreenWidthOrWider get() = appState.isMediumScreenWidthOrWider

    internal var density by mutableStateOf(density)

    internal val canShowBottomNavigation get() = !appState.isMediumScreenWidthOrWider

    internal val canShowNavRail
        get() = appState.filteredPaneOrder.firstOrNull() == paneState.pane
                && appState.isMediumScreenWidthOrWider

    internal val canShowFab
        get() = when (paneState.pane) {
            ThreePane.Primary -> true
            ThreePane.TransientPrimary -> true
            ThreePane.Secondary -> false
            ThreePane.Tertiary -> false
            ThreePane.Overlay -> false
            null -> false
        }

    internal var scaffoldTargetSize by mutableStateOf(IntSize.Zero)
    internal var scaffoldCurrentSize by mutableStateOf(IntSize.Zero)

    internal fun hasMatchedSize(): Boolean =
        abs(scaffoldCurrentSize.width - scaffoldTargetSize.width) <= 2
                && abs(scaffoldCurrentSize.height - scaffoldTargetSize.height) <= 2
}

@Composable
fun PaneScope<ThreePane, Route>.rememberPaneScaffoldState(): PaneScaffoldState {
    val density = LocalDensity.current
    val appState = LocalAppState.current
    val paneMovableElementSharedTransitionScope = rememberPaneMovableElementSharedTransitionScope()
    return remember(appState) {
        PaneScaffoldState(
            appState = appState,
            paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
            density = density,
        )
    }
        .also { it.density = density }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PaneScaffoldState.PaneScaffold(
    modifier: Modifier = Modifier,
    showNavigation: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.background,
    snackBarMessages: List<String> = emptyList(),
    onSnackBarMessageConsumed: (String) -> Unit,
    topBar: @Composable PaneScaffoldState.() -> Unit = {},
    floatingActionButton: @Composable PaneScaffoldState.() -> Unit = {},
    navigationBar: @Composable PaneScaffoldState.() -> Unit = {},
    navigationRail: @Composable PaneScaffoldState.() -> Unit = {},
    content: @Composable PaneScaffoldState.(PaddingValues) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    PaneNavigationRailScaffold(
        modifier = modifier,
        navigationRail = {
            navigationRail()
        },
        content = {
            Scaffold(
                modifier = Modifier
                    .animateBounds(
                        lookaheadScope = this,
                        boundsTransform = remember {
                            scaffoldBoundsTransform(
                                appState = appState,
                                paneScaffoldState = this,
                            )
                        }
                    )
                    .padding(
                        horizontal = if (appState.filteredPaneOrder.size > 1) 8.dp else 0.dp
                    )
                    .onSizeChanged {
                        scaffoldCurrentSize = it
                    },
                containerColor = containerColor,
                topBar = {
                    topBar()
                },
                floatingActionButton = {
                    floatingActionButton()
                },
                bottomBar = {
                    navigationBar()
                },
                snackbarHost = {
                    SnackbarHost(snackbarHostState)
                },
                content = { paddingValues ->
                    content(paddingValues)
                },
            )
        }
    )
    val updatedMessages = rememberUpdatedState(snackBarMessages.firstOrNull())
    LaunchedEffect(Unit) {
        snapshotFlow { updatedMessages.value }
            .filterNotNull()
            .filterNot(String::isNullOrBlank)
            .collect { message ->
                snackbarHostState.showSnackbar(
                    message = message
                )
                onSnackBarMessageConsumed(message)
            }
    }

    if (paneState.pane == ThreePane.Primary) {
        LaunchedEffect(showNavigation) {
            appState.showNavigation = showNavigation
        }
    }
}

@Composable
private inline fun PaneNavigationRailScaffold(
    modifier: Modifier = Modifier,
    navigationRail: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        content = {
            Box(
                modifier = Modifier
                    .widthIn(max = 80.dp)
                    .zIndex(2f),
                content = {
                    navigationRail()
                },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f),
                content = {
                    content()
                }
            )
        },
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
private fun scaffoldBoundsTransform(
    appState: AppState,
    paneScaffoldState: PaneScaffoldState,
): BoundsTransform = BoundsTransform { _, targetBounds ->
    paneScaffoldState.scaffoldTargetSize =
        targetBounds.size.roundToIntSize()

    when (paneScaffoldState.paneState.pane) {
        ThreePane.Primary,
        ThreePane.Secondary,
        ThreePane.Tertiary,
            -> if (appState.paneAnchorState.hasInteractions) snap()
        else spring()

        ThreePane.TransientPrimary,
            -> spring<Rect>().skipIf(paneScaffoldState::hasMatchedSize)

        ThreePane.Overlay,
        null,
            -> snap()
    }
}

fun Modifier.paneClip() =
    then(PaneClipModifier)

private val PaneClipModifier = Modifier.clip(
    shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
    )
)