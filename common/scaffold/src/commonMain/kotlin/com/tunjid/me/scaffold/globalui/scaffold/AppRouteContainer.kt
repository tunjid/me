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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.zIndex
import com.tunjid.me.core.utilities.countIf
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.UiSizes
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.keyboardSize
import com.tunjid.me.scaffold.globalui.slices.routeContainerState
import com.tunjid.me.scaffold.lifecycle.mappedCollectAsStateWithLifecycle
import com.tunjid.me.scaffold.nav.NavStateHolder

/**
 * Motionally intelligent container for the hosting the navigation routes
 */
@Composable
internal fun AppRouteContainer(
    globalUiStateHolder: GlobalUiStateHolder,
    navStateHolder: NavStateHolder,
    mainContent: @Composable () -> Unit,
    navRailContent: @Composable () -> Unit,
) {
    val state by globalUiStateHolder.state.mappedCollectAsStateWithLifecycle(
        mapper = UiState::routeContainerState
    )

    val bottomNavHeight = UiSizes.bottomNavSize countIf state.bottomNavVisible

    val insetClearance = max(
        a = bottomNavHeight,
        b = with(LocalDensity.current) { state.keyboardSize.toDp() }
    )
    val navBarClearance = with(LocalDensity.current) {
        state.navBarSize.toDp()
    } countIf state.insetDescriptor.hasBottomInset

    val bottomClearance by animateDpAsState(targetValue = insetClearance + navBarClearance)

    val statusBarSize = with(LocalDensity.current) {
        state.statusBarSize.toDp()
    } countIf state.insetDescriptor.hasTopInset

    val toolbarHeight = UiSizes.toolbarSize countIf !state.toolbarOverlaps
    val topClearance by animateDpAsState(targetValue = statusBarSize + toolbarHeight)

    val hasNavContent by navStateHolder.state.mappedCollectAsStateWithLifecycle { it.navRail != null }
    val navRailVisible = state.navRailVisible

    val navRailSize = UiSizes.navRailWidth countIf navRailVisible

    val startClearance by animateDpAsState(targetValue = navRailSize)

    val navAnimations = remember {
        MutableNavAnimations()
    }

    CompositionLocalProvider(
        LocalNavigationAnimator provides navAnimations,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = startClearance,
                    top = topClearance,
                    bottom = bottomClearance
                ),
            content = {
                val hasNarrowWidth = maxWidth - UiSizes.navRailContentWidth < 300.dp
                val targetNavWidth = when {
                    hasNarrowWidth -> maxWidth
                    hasNavContent -> UiSizes.navRailContentWidth
                    else -> maxWidth
                }

                ResizableRouteContent(
                    zIndex = 2f,
                    width = maxWidth,
                    startPadding = when {
                        hasNarrowWidth -> 0.dp
                        hasNavContent -> UiSizes.navRailContentWidth
                        else -> 0.dp
                    },
                    content = mainContent
                )
                val navWidth by animateDpAsState(
                    targetValue = targetNavWidth,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessLow
                    )
                )
                ResizableRouteContent(
                    zIndex = if (hasNarrowWidth) 1f else 3f,
                    width = navWidth,
                    content = navRailContent
                )

                LaunchedEffect(navWidth, hasNavContent) {
                    val difference = (navWidth - targetNavWidth).let { if (it < 0.dp) it * -1 else it }
                    val percentage = difference / targetNavWidth
                    navAnimations.isAnimatingNavRail.value = percentage >= 0.05f
                }
            }
        )
    }
}

@Composable
private fun ResizableRouteContent(
    zIndex: Float,
    width: Dp? = null,
    startPadding: Dp? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = when (width) {
            null -> Modifier.fillMaxWidth()
            else -> Modifier.width(width)
        }
            .zIndex(zIndex)
            .padding(start = startPadding ?: 0.dp)
            .background(color = MaterialTheme.colors.surface),
        content = {
            content()
        }
    )
}

val LocalNavigationAnimator = staticCompositionLocalOf<NavAnimations> {
    MutableNavAnimations().apply {
        isAnimatingNavRail.value = false
    }
}

interface NavAnimations {
    val isAnimatingNavRail: State<Boolean>
}

private class MutableNavAnimations : NavAnimations {
    override var isAnimatingNavRail = mutableStateOf(false)
}