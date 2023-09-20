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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.tunjid.me.core.utilities.countIf
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.WindowSizeClass
import com.tunjid.me.scaffold.globalui.bottomNavSize
import com.tunjid.me.scaffold.globalui.isNotExpanded
import com.tunjid.me.scaffold.globalui.keyboardSize
import com.tunjid.me.scaffold.globalui.navRailWidth
import com.tunjid.me.scaffold.globalui.slices.routeContainerState
import com.tunjid.me.scaffold.globalui.secondaryContentWidth
import com.tunjid.me.scaffold.globalui.toolbarSize
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
    primaryContent: @Composable () -> Unit,
    secondaryContent: @Composable () -> Unit,
) {
    val paddingValues = routeContainerPadding(globalUiStateHolder)
    val (startClearance, topClearance, _, bottomClearance) = paddingValues

    val windowSizeClass by globalUiStateHolder.state.mappedCollectAsStateWithLifecycle {
        it.windowSizeClass
    }
    val hasNavContent by navStateHolder.state.mappedCollectAsStateWithLifecycle {
        it.secondaryRoute != null
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = startClearance,
                top = topClearance,
                bottom = bottomClearance
            ),
        content = {
            Box(
                modifier = Modifier
                    .width(
                        animateWidthChange(
                            if (hasNavContent) WindowSizeClass.EXPANDED.secondaryContentWidth()
                            else maxWidth
                        ).value
                    )
                    .background(color = MaterialTheme.colorScheme.surface),
                content = { secondaryContent() }
            )
            Box(
                modifier = Modifier
                    .width(
                        primaryContentWidth(
                            windowSizeClass = windowSizeClass,
                            moveKind = moveKind
                        ).value
                    )
                    .padding(
                        start = when {
                            hasNavContent -> animateWidthChange(
                                targetSideWidth = windowSizeClass.secondaryContentWidth()
                            ).value

                            else -> 0.dp
                        }
                    )
                    .background(color = MaterialTheme.colorScheme.surface),
                content = { primaryContent() }
            )
        }
    )
}

@Composable
private fun BoxWithConstraintsScope.primaryContentWidth(
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
        if (moveKind == MoveKind.SecondaryToPrimary && !moveComplete) {
            value = windowSizeClass.secondaryContentWidth()

            val anim = TargetBasedAnimation(
                animationSpec = secondaryContentSizeSpring(),
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
private fun animateWidthChange(targetSideWidth: Dp) = animateDpAsState(
    targetValue = targetSideWidth,
    animationSpec = secondaryContentSizeSpring()
)

private fun secondaryContentSizeSpring() = spring<Dp>(
    stiffness = Spring.StiffnessMediumLow
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
