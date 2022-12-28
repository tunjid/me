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
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.tunjid.me.core.utilities.countIf
import com.tunjid.me.scaffold.globalui.*
import com.tunjid.me.scaffold.globalui.slices.routeContainerState
import com.tunjid.me.scaffold.lifecycle.mappedCollectAsStateWithLifecycle
import com.tunjid.me.scaffold.nav.MoveKind
import com.tunjid.me.scaffold.nav.NavStateHolder

/**
 * Motionally intelligent container for the hosting the navigation routes
 */
@Composable
internal fun AppRouteContainer(
    globalUiStateHolder: GlobalUiStateHolder,
    navStateHolder: NavStateHolder,
    moveKind: MoveKind,
    mainContent: @Composable () -> Unit,
    supportingContent: @Composable () -> Unit,
) {
    val paddingValues = routeContainerPadding(globalUiStateHolder)
    val (startClearance, topClearance, _, bottomClearance) = paddingValues

    val windowSizeClass by globalUiStateHolder.state.mappedCollectAsStateWithLifecycle {
        it.windowSizeClass
    }
    val hasNavContent by navStateHolder.state.mappedCollectAsStateWithLifecycle {
        it.supportingRoute != null
    }
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
                val mainContentWidth by mainContentWidth(
                    windowSizeClass = windowSizeClass,
                    moveKind = moveKind
                )
                ResizableRouteContent(
                    width = mainContentWidth,
                    startPadding = when {
                        hasNavContent -> windowSizeClass.supportingPanelWidth()
                        else -> 0.dp
                    },
                    content = mainContent
                )

                val targetSupportingContentWidth = when {
                    hasNavContent -> windowSizeClass.supportingPanelWidth()
                    else -> maxWidth
                }
                val supportingContentWidth by supportingContentWidth(
                    targetSupportingContentWidth
                )
                if (supportingContentWidth != 0.dp) ResizableRouteContent(
                    width = supportingContentWidth,
                    content = supportingContent
                )

                LaunchedEffect(supportingContentWidth, hasNavContent) {
                    val difference = (supportingContentWidth - targetSupportingContentWidth).let {
                        if (it < 0.dp) it * -1 else it
                    }
                    val percentage = difference / targetSupportingContentWidth
                    navAnimations.isAnimatingSupportingContent.value = percentage >= 0.01f
                }
            }
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.mainContentWidth(
    windowSizeClass: WindowSizeClass,
    moveKind: MoveKind
): State<Dp> {
    if (windowSizeClass.isNotExpanded) return mutableStateOf(maxWidth)

    var moveComplete by remember(moveKind) {
        mutableStateOf(false)
    }
    return produceState(
        initialValue = maxWidth,
        key1 = maxWidth,
        key2 = moveKind,
        key3 = moveComplete,
    ) {
        if (moveKind == MoveKind.SupportingToMain && !moveComplete) {
            value = windowSizeClass.supportingPanelWidth()

            val anim = TargetBasedAnimation(
                animationSpec = navContentSizeSpring(),
                typeConverter = Dp.VectorConverter,
                initialValue = value,
                targetValue = maxWidth
            )

            var playTime: Long
            val startTime = withFrameNanos { it }

            while (value < maxWidth) {
                playTime = withFrameNanos { it } - startTime
                value = anim.getValueFromNanos(playTime)
            }
            moveComplete = true
        } else value = maxWidth
    }
}

@Composable
private fun supportingContentWidth(targetSideWidth: Dp) = animateDpAsState(
    targetValue = targetSideWidth,
    animationSpec = navContentSizeSpring()
)

@Composable
private fun ResizableRouteContent(
    width: Dp,
    startPadding: Dp? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(start = startPadding ?: 0.dp)
            .background(color = MaterialTheme.colors.surface),
        content = {
            content()
        }
    )
}

private fun navContentSizeSpring() = spring<Dp>(
    stiffness = Spring.StiffnessLow
)

@Composable
private fun routeContainerPadding(
    globalUiStateHolder: GlobalUiStateHolder,
): SnapshotStateList<Dp> {
    val paddingValues = remember {
        mutableStateListOf(0.dp, 0.dp, 0.dp, 0.dp)
    }
    val state by globalUiStateHolder.state.mappedCollectAsStateWithLifecycle(
        mapper = UiState::routeContainerState
    )

    val bottomNavHeight = state.windowSizeClass.bottomNavSize() countIf state.bottomNavVisible

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

    val toolbarHeight = state.windowSizeClass.toolbarSize() countIf !state.toolbarOverlaps
    val topClearance by animateDpAsState(targetValue = statusBarSize + toolbarHeight)

    val navRailSize = state.windowSizeClass.navRailWidth()

    val startClearance by animateDpAsState(targetValue = navRailSize)

    paddingValues[0] = startClearance
    paddingValues[1] = topClearance
    paddingValues[3] = bottomClearance

    return paddingValues
}

val LocalNavigationAnimator = staticCompositionLocalOf<NavAnimations> {
    MutableNavAnimations().apply {
        isAnimatingSupportingContent.value = false
    }
}

interface NavAnimations {
    val isAnimatingSupportingContent: State<Boolean>
}

private class MutableNavAnimations : NavAnimations {
    override var isAnimatingSupportingContent = mutableStateOf(false)
}