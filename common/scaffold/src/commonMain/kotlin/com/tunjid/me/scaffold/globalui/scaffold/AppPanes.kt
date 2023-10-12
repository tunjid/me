package com.tunjid.me.scaffold.globalui.scaffold

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.SwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.tunjid.me.scaffold.globalui.BackHandler
import com.tunjid.me.scaffold.globalui.PaneAnchor
import com.tunjid.me.scaffold.globalui.progress
import kotlin.math.max
import kotlin.math.roundToInt

internal val LocalPaneAnchorState = staticCompositionLocalOf {
    PaneAnchorState()
}

@Stable
// TODO: Migrate to AnchoredDraggable when moving to Compose 1.6
internal class PaneAnchorState {
    var maxWidth by mutableIntStateOf(1000)
        internal set
    val width
        get() = max(
            a = 0,
            b = swipeableState.offset.value.roundToInt()
        )

    val targetPaneAnchor get() = swipeableState.targetValue

    val currentPaneAnchor get() = swipeableState.currentValue

    private val thumbMutableInteractionSource = MutableInteractionSource()

    val thumbInteractionSource: InteractionSource = thumbMutableInteractionSource

    private val swipeableState = SwipeableState(
        initialValue = PaneAnchor.OneThirds,
        animationSpec = tween(),
    )

    val modifier by derivedStateOf {
        Modifier
            .hoverable(thumbMutableInteractionSource)
            .swipeable(
                state = swipeableState,
                anchors = mapOf(
                    0f to PaneAnchor.Zero,
                    (maxWidth * (1f / 3)) to PaneAnchor.OneThirds,
                    (maxWidth * (1f / 2)) to PaneAnchor.Half,
                    (maxWidth * (2f / 3)) to PaneAnchor.TwoThirds,
                    maxWidth.toFloat() to PaneAnchor.Full,
                ),
                orientation = Orientation.Horizontal,
                interactionSource = thumbMutableInteractionSource,
            )
    }

    fun updateMaxWidth(maxWidth: Int) {
        this.maxWidth = maxWidth
    }

    fun dispatch(delta: Float) {
        swipeableState.performDrag(delta)
    }

    suspend fun completeDispatch() = swipeableState.performFling(velocity = 0f)

    suspend fun moveTo(anchor: PaneAnchor) = swipeableState.animateTo(anchor)
}

/**
 * Maps a back gesture to shutting the secondary pane
 */
@Composable
fun SecondaryPaneCloseBackHandler(enabled: Boolean) {
    val paneSplitState = LocalPaneAnchorState.current
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
