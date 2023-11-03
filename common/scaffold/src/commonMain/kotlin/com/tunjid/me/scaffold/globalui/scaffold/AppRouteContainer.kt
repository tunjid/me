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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.zIndex
import com.tunjid.me.core.utilities.countIf
import com.tunjid.me.scaffold.adaptiveSpringSpec
import com.tunjid.me.scaffold.globalui.PaneAnchor
import com.tunjid.me.scaffold.globalui.WindowSizeClass
import com.tunjid.me.scaffold.globalui.WindowSizeClass.COMPACT
import com.tunjid.me.scaffold.globalui.adaptive.Adaptive
import com.tunjid.me.scaffold.globalui.adaptive.Adaptive.Adaptation.Companion.PrimaryToSecondary
import com.tunjid.me.scaffold.globalui.adaptive.Adaptive.Adaptation.Companion.SecondaryToPrimary
import com.tunjid.me.scaffold.globalui.bottomNavSize
import com.tunjid.me.scaffold.globalui.keyboardSize
import com.tunjid.me.scaffold.globalui.navRailWidth
import com.tunjid.me.scaffold.globalui.slices.RouteContainerPositionalState
import com.tunjid.me.scaffold.globalui.toolbarSize
import com.tunjid.me.scaffold.nav.ExpandAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Motionally intelligent, adaptive container for the hosting the navigation routes
 */
@Composable
internal fun AppRouteContainer(
    state: Adaptive.NavigationState,
    onPaneAnchorChanged: (PaneAnchor) -> Unit,
    primaryContent: @Composable () -> Unit,
    secondaryContent: @Composable () -> Unit,
    transientPrimaryContent: @Composable () -> Unit,
) {
    val paddingValues = routeContainerPadding(state.routeContainerPositionalState)
    val (startClearance, topClearance, _, bottomClearance) = paddingValues

    val updatedState by rememberUpdatedState(state)

    val hasNavContent by remember {
        derivedStateOf { updatedState.secondaryRoute != null }
    }
    val windowSizeClass by remember {
        derivedStateOf { updatedState.windowSizeClass }
    }
    val adaptation by remember {
        derivedStateOf { updatedState.adaptation }
    }
    val density = LocalDensity.current
    val paneSplitState = remember(::PaneAnchorState)

    CompositionLocalProvider(
        LocalPaneAnchorState provides paneSplitState,
    ) {
        Box(
            modifier = Modifier
                // Place under bottom navigation, app bars, fabs and the like
                .zIndex(PaneDragHandleZIndex)
                .fillMaxWidth()
                .onSizeChanged { paneSplitState.updateMaxWidth(it.width) }
                .padding(
                    start = startClearance,
                    top = topClearance,
                    bottom = bottomClearance
                ),
            content = {
                SecondaryContentContainer(
                    modifier = secondaryContentModifier(
                        adaptation = adaptation,
                        width = with(density) { paneSplitState.width.toDp() },
                        maxWidth = with(density) { paneSplitState.maxWidth.toDp() },
                    ),
                    secondaryContent = secondaryContent
                )
                PrimaryContentContainer(
                    modifier = primaryContentModifier(
                        windowSizeClass = windowSizeClass,
                        adaptation = adaptation,
                        secondaryContentWidth = with(density) { paneSplitState.width.toDp() },
                        maxWidth = with(density) { paneSplitState.maxWidth.toDp() }
                    ),
                    primaryContent = primaryContent,
                    transientPrimaryContent = transientPrimaryContent,
                )
                AnimatedVisibility(
                    modifier = Modifier.align(Alignment.CenterStart),
                    visible = hasNavContent && windowSizeClass > COMPACT
                ) {
                    DraggableThumb(
                        paneAnchorState = paneSplitState
                    )
                }
            }
        )
    }

    LaunchedEffect(windowSizeClass, hasNavContent) {
        // Delay briefly so the animation runs
        delay(5)
        paneSplitState.moveTo(
            if (hasNavContent) when (windowSizeClass) {
                COMPACT -> PaneAnchor.Zero
                WindowSizeClass.MEDIUM -> PaneAnchor.OneThirds
                WindowSizeClass.EXPANDED -> PaneAnchor.Half
            }
            else PaneAnchor.Zero
        )
    }
    LaunchedEffect(paneSplitState.currentPaneAnchor) {
        onPaneAnchorChanged(paneSplitState.currentPaneAnchor)
    }
}

@Composable
private fun PrimaryContentContainer(
    modifier: Modifier,
    primaryContent: @Composable () -> Unit,
    transientPrimaryContent: @Composable () -> Unit
) {
    Box(
        modifier = modifier,
        content = {
            primaryContent()
            transientPrimaryContent()
        }
    )
}

@Composable
private fun SecondaryContentContainer(
    modifier: Modifier,
    secondaryContent: @Composable () -> Unit
) {
    Box(
        modifier = modifier,
        content = { secondaryContent() }
    )
}

@Composable
private fun BoxScope.DraggableThumb(
    paneAnchorState: PaneAnchorState
) {
    val scope = rememberCoroutineScope()
    val isHovered by paneAnchorState.thumbInteractionSource.collectIsHoveredAsState()
    val isPressed by paneAnchorState.thumbInteractionSource.collectIsPressedAsState()
    val isDragged by paneAnchorState.thumbInteractionSource.collectIsDraggedAsState()
    val thumbWidth by animateDpAsState(
        if (isHovered || isPressed || isDragged) DraggableDividerSizeDp
        else when (paneAnchorState.targetPaneAnchor) {
            PaneAnchor.Zero -> DraggableDividerSizeDp
            PaneAnchor.OneThirds,
            PaneAnchor.Half,
            PaneAnchor.TwoThirds,
            PaneAnchor.Full -> 2.dp
        }
    )
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = paneAnchorState.width - (DraggableDividerSizeDp / 2).roundToPx(),
                    y = 0
                )
            }
            .align(Alignment.CenterStart)
            .width(DraggableDividerSizeDp)
            .then(paneAnchorState.modifier)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .width(thumbWidth)
                .height(DraggableDividerSizeDp),
            shape = RoundedCornerShape(DraggableDividerSizeDp),
            onClick = {
                scope.launch { paneAnchorState.moveTo(PaneAnchor.OneThirds) }
            },
        ) {
            Image(
                modifier = Modifier
                    .align(Alignment.Center)
                    .scale(0.6f),
                imageVector = Icons.Filled.ExpandAll,
                contentDescription = "Drag",
                colorFilter = ColorFilter.tint(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun primaryContentModifier(
    windowSizeClass: WindowSizeClass,
    adaptation: Adaptive.Adaptation,
    secondaryContentWidth: Dp,
    maxWidth: Dp,
): Modifier {
    val updatedSecondaryContentWidth by rememberUpdatedState(secondaryContentWidth)
    val widthAnimatable = remember {
        Animatable(
            initialValue = maxWidth,
            typeConverter = Dp.VectorConverter,
            visibilityThreshold = Dp.VisibilityThreshold,
        )
    }
    var complete by remember { mutableStateOf(false) }

    LaunchedEffect(windowSizeClass, adaptation) {
        try {
            // Maintain max width on smaller devices
            if (windowSizeClass == COMPACT || adaptation != SecondaryToPrimary
            ) {
                complete = true
                return@LaunchedEffect
            }
            complete = false
            // Snap to this width to give the impression of the container sliding
            widthAnimatable.snapTo(targetValue = updatedSecondaryContentWidth)
            widthAnimatable.animateTo(
                targetValue = maxWidth,
                animationSpec = ContentSizeSpring,
            )
        } finally {
            complete = true
        }
    }

    return Modifier
        .zIndex(PrimaryContainerZIndex)
        .width(if (complete) maxWidth else widthAnimatable.value)
        .padding(
            start = updatedSecondaryContentWidth.countIf(
                condition = adaptation != SecondaryToPrimary && windowSizeClass != COMPACT
            )
        )
        .restrictedSizePlacement(
            atStart = adaptation == SecondaryToPrimary
        )
}

@Composable
private fun secondaryContentModifier(
    adaptation: Adaptive.Adaptation,
    width: Dp,
    maxWidth: Dp,
): Modifier {
    val updatedWidth = rememberUpdatedState(width)
    val updatedMaxWidth = rememberUpdatedState(maxWidth)
    val widthAnimatable = remember {
        Animatable(
            initialValue = maxWidth,
            typeConverter = Dp.VectorConverter,
            visibilityThreshold = Dp.VisibilityThreshold,
        )
    }
    var complete by remember { mutableStateOf(true) }

    LaunchedEffect(adaptation) {
        complete = adaptation != PrimaryToSecondary
    }

    LaunchedEffect(complete) {
        if (complete) return@LaunchedEffect
        snapshotFlow { updatedWidth.value }
            .collectLatest { newestWidth ->
                // Don't cancel the previous animation, instead launch a new one and let the
                // animatable handle the retargeting required
                launch {
                    if (widthAnimatable.value != newestWidth) widthAnimatable.animateTo(
                        targetValue = newestWidth,
                        animationSpec = ContentSizeSpring,
                    )
                    complete = true
                    // Keep the animatable width at the full width for seamless animations
                    widthAnimatable.snapTo(targetValue = updatedMaxWidth.value)
                }
            }
    }

    return Modifier
        // Display the secondary content over the primary content to maintain the sliding illusion
        .zIndex(if (complete) SecondaryContainerZIndex else SecondaryContainerAnimationZIndex)
        .width(if (complete) updatedWidth.value else widthAnimatable.value)
        .restrictedSizePlacement(
            atStart = adaptation == PrimaryToSecondary
        )
}

@Composable
private fun routeContainerPadding(
    state: RouteContainerPositionalState,
): SnapshotStateList<Dp> {
    val paddingValues = remember {
        mutableStateListOf(0.dp, 0.dp, 0.dp, 0.dp)
    }

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

/**
 * Shifts layouts out of view when the content area is too small instead of resizing them
 */
private fun Modifier.restrictedSizePlacement(
    atStart: Boolean
) = layout { measurable, constraints ->
    val minPanWidth = MinPaneWidth.roundToPx()
    val actualConstraints = when {
        constraints.maxWidth < minPanWidth -> constraints.copy(maxWidth = minPanWidth)
        else -> constraints
    }
    val placeable = measurable.measure(actualConstraints)
    layout(width = placeable.width, height = placeable.height) {
        placeable.placeRelative(
            x = when {
                constraints.maxWidth < minPanWidth -> when {
                    atStart -> constraints.maxWidth - minPanWidth
                    else -> minPanWidth - constraints.maxWidth
                }

                else -> 0
            },
            y = 0
        )
    }
}

private val DraggableDividerSizeDp = 48.dp
private val MinPaneWidth = 120.dp

private val ContentSizeSpring = adaptiveSpringSpec(
    visibilityThreshold = Dp.VisibilityThreshold
)

private const val PaneDragHandleZIndex = -1f
private const val PrimaryContainerZIndex = -2f
private const val SecondaryContainerZIndex = -3f
private const val SecondaryContainerAnimationZIndex = -1.5f