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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.material.SwipeableState
import androidx.compose.material.icons.Icons
import androidx.compose.material.swipeable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
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
import com.tunjid.me.scaffold.globalui.BackHandler
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.PaneSplit
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.WindowSizeClass
import com.tunjid.me.scaffold.globalui.bottomNavSize
import com.tunjid.me.scaffold.globalui.isNotExpanded
import com.tunjid.me.scaffold.globalui.keyboardSize
import com.tunjid.me.scaffold.globalui.navRailWidth
import com.tunjid.me.scaffold.globalui.progress
import com.tunjid.me.scaffold.globalui.slices.routeContainerState
import com.tunjid.me.scaffold.globalui.toolbarSize
import com.tunjid.me.scaffold.lifecycle.mappedCollectAsStateWithLifecycle
import com.tunjid.me.scaffold.nav.ExpandAll
import com.tunjid.me.scaffold.nav.MoveKind
import com.tunjid.me.scaffold.nav.NavStateHolder
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

private val LocalPaneSplitState = staticCompositionLocalOf {
    PaneSplitState()
}

@Stable
// TODO: Migrate to AnchoredDraggable when moving to Compose 1.6
private class PaneSplitState {
    var maxWidth by mutableIntStateOf(1000)
        private set
    val width
        get() = max(
            a = 0,
            b = swipeableState.offset.value.roundToInt()
        )

    val targetPaneSplit get() = swipeableState.targetValue

    val currentPaneSplit get() = swipeableState.currentValue

    private val mutableInteractionSource = MutableInteractionSource()

    val interactionSource: InteractionSource = mutableInteractionSource

    private val swipeableState = SwipeableState(
        initialValue = PaneSplit.OneThirds,
        animationSpec = tween(),
    )

    val modifier by derivedStateOf {
        Modifier.swipeable(
            state = swipeableState,
            anchors = mapOf(
                0f to PaneSplit.Zero,
                (maxWidth * (1f / 3)) to PaneSplit.OneThirds,
                (maxWidth * (1f / 2)) to PaneSplit.Half,
                (maxWidth * (2f / 3)) to PaneSplit.TwoThirds,
                maxWidth.toFloat() to PaneSplit.Full,
            ),
            orientation = Orientation.Horizontal,
            interactionSource = mutableInteractionSource,
        )
    }

    fun updateMaxWidth(maxWidth: Int) {
        this.maxWidth = maxWidth
    }

    fun dispatch(delta: Float) {
        swipeableState.performDrag(delta)
    }

    suspend fun completeDispatch() = swipeableState.performFling(0f)

    suspend fun moveTo(anchor: PaneSplit) = swipeableState.animateTo(anchor)
}

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
    transientPrimaryContent: @Composable () -> Unit,
) {
    val paddingValues = routeContainerPadding(globalUiStateHolder)
    val (startClearance, topClearance, _, bottomClearance) = paddingValues

    val windowSizeClass by globalUiStateHolder.state.mappedCollectAsStateWithLifecycle {
        it.windowSizeClass
    }
    val hasNavContent by navStateHolder.state.mappedCollectAsStateWithLifecycle {
        it.secondaryRoute != null
    }
    val density = LocalDensity.current
    val paneSplitState = remember(::PaneSplitState)

    CompositionLocalProvider(
        LocalPaneSplitState provides paneSplitState,
    ) {
        Box(
            modifier = Modifier
                .zIndex(-1f)
                .fillMaxWidth()
                .onSizeChanged { paneSplitState.updateMaxWidth(it.width) }
                .padding(
                    start = startClearance,
                    top = topClearance,
                    bottom = bottomClearance
                ),
            content = {
                SecondaryContainer(
                    hasNavContent = hasNavContent,
                    width = with(density) { paneSplitState.width.toDp() },
                    maxWidth = with(density) { paneSplitState.maxWidth.toDp() },
                    secondaryContent = secondaryContent
                )
                PrimaryContainer(
                    windowSizeClass = windowSizeClass,
                    moveKind = moveKind,
                    secondaryContentWidth = with(density) { paneSplitState.width.toDp() },
                    maxWidth = with(density) { paneSplitState.maxWidth.toDp() },
                    hasNavContent = hasNavContent,
                    primaryContent = primaryContent,
                    transientPrimaryContent = transientPrimaryContent,
                )
                AnimatedVisibility(
                    modifier = Modifier.align(Alignment.CenterStart),
                    visible = hasNavContent && windowSizeClass > WindowSizeClass.COMPACT
                ) {
                    DraggableThumb(
                        paneSplitState = paneSplitState
                    )
                }
            }
        )
    }

    LaunchedEffect(windowSizeClass, hasNavContent) {
        paneSplitState.moveTo(
            if (hasNavContent) when (windowSizeClass) {
                WindowSizeClass.COMPACT -> PaneSplit.Zero
                WindowSizeClass.MEDIUM -> PaneSplit.OneThirds
                WindowSizeClass.EXPANDED -> PaneSplit.Half
            }
            else PaneSplit.Zero
        )
    }

    LaunchedEffect(paneSplitState.currentPaneSplit) {
        globalUiStateHolder.accept {
            copy(paneSplit = paneSplitState.currentPaneSplit)
        }
    }
}

@Composable
private fun PrimaryContainer(
    windowSizeClass: WindowSizeClass,
    moveKind: MoveKind,
    maxWidth: Dp,
    secondaryContentWidth: Dp,
    hasNavContent: Boolean,
    primaryContent: @Composable () -> Unit,
    transientPrimaryContent: @Composable () -> Unit
) {
    val animatedWidth by primaryContentWidth(
        windowSizeClass = windowSizeClass,
        moveKind = moveKind,
        maxWidth = maxWidth,
        secondaryContentWidth = max(
            a = MinPaneWidth,
            b = secondaryContentWidth
        )
    )
    val startPadding = if (hasNavContent) secondaryContentWidth else 0.dp

    val baseModifier = Modifier
        .width(animatedWidth)
        .padding(start = startPadding)
        // Do not place items when they are too small, but keep them in the composition
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) innerLayout@{
                if (placeable.width.toDp() < MinPaneWidth) return@innerLayout
                placeable.place(x = 0, y = 0)
            }
        }
    Box(
        modifier = baseModifier
            .background(color = MaterialTheme.colorScheme.surface),
        content = { primaryContent() }
    )
    Box(
        modifier = baseModifier,
        content = { transientPrimaryContent() }
    )
}

@Composable
private fun SecondaryContainer(
    hasNavContent: Boolean,
    width: Dp,
    maxWidth: Dp,
    secondaryContent: @Composable () -> Unit
) {
    val actualWidth = max(
        a = MinPaneWidth,
        b = if (hasNavContent) width else maxWidth
    )
    Box(
        modifier = Modifier
            .width(actualWidth)
            .background(color = MaterialTheme.colorScheme.surface),
        content = { secondaryContent() }
    )
}

@Composable
private fun BoxScope.DraggableThumb(
    paneSplitState: PaneSplitState
) {
    val scope = rememberCoroutineScope()
    val isPressed by paneSplitState.interactionSource.collectIsPressedAsState()
    val isDragged by paneSplitState.interactionSource.collectIsDraggedAsState()
    val thumbWidth by animateDpAsState(
        if (isPressed || isDragged) DraggableDividerSizeDp
        else when (paneSplitState.targetPaneSplit) {
            PaneSplit.Zero -> DraggableDividerSizeDp
            PaneSplit.OneThirds,
            PaneSplit.Half,
            PaneSplit.TwoThirds,
            PaneSplit.Full -> 2.dp
        }
    )
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = paneSplitState.width - (DraggableDividerSizeDp / 2).roundToPx(),
                    y = 0
                )
            }
            .align(Alignment.CenterStart)
            .width(DraggableDividerSizeDp)
            .then(paneSplitState.modifier)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .width(thumbWidth)
                .height(DraggableDividerSizeDp),
            shape = RoundedCornerShape(DraggableDividerSizeDp),
            onClick = {
                scope.launch { paneSplitState.moveTo(PaneSplit.OneThirds) }
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
private fun primaryContentWidth(
    windowSizeClass: WindowSizeClass,
    moveKind: MoveKind,
    secondaryContentWidth: Dp,
    maxWidth: Dp,
): State<Dp> {
    if (windowSizeClass.isNotExpanded) return mutableStateOf(maxWidth)
    val updatedSecondaryContentWidth by rememberUpdatedState(secondaryContentWidth)

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
            value = updatedSecondaryContentWidth

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

@Composable
fun SeconaryPaneCloseBackHandler(enabled: Boolean) {
    val paneSplitState = LocalPaneSplitState.current
    var started by remember { mutableStateOf(false) }
    var widthAtStart by remember { mutableIntStateOf(0) }
    var desiredPaneWidth by remember { mutableFloatStateOf(0f) }
    val animatedDesiredPanelWidth by animateFloatAsState(desiredPaneWidth)

    BackHandler(
        enabled = enabled,
        onStarted = {
            widthAtStart = paneSplitState.width
            started = true
        },
        onProgressed = { backStatus ->
            val backProgress = backStatus.progress
            val distanceToCover = paneSplitState.maxWidth - widthAtStart
            desiredPaneWidth = (backProgress * distanceToCover) + widthAtStart
        },
        onCancelled = {
            started = false
        },
        onBack = {
            started = false
        }
    )

    // Make sure desiredPaneWidth is synced with paneSplitState.width before the back gesture
    LaunchedEffect(started, paneSplitState.width) {
        if (started) return@LaunchedEffect
        desiredPaneWidth = paneSplitState.width.toFloat()
    }

    // Dispatch changes as the user presses back
    LaunchedEffect(started, animatedDesiredPanelWidth) {
        if (!started) return@LaunchedEffect
        paneSplitState.dispatch(delta = animatedDesiredPanelWidth - paneSplitState.width.toFloat())
    }

    // Fling to settle
    LaunchedEffect(started) {
        if (started) return@LaunchedEffect
        paneSplitState.completeDispatch()
    }
}

private val DraggableDividerSizeDp = 48.dp
private val MinPaneWidth = 120.dp
