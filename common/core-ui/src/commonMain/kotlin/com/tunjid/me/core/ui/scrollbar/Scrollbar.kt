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

package com.tunjid.me.core.ui.scrollbar

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay

@Immutable
@JvmInline
value class ScrollbarState internal constructor(
    internal val packedValue: Long
) {
    companion object {
        val FULL = ScrollbarState(
            thumbSizePercent = 1f,
            thumbTravelPercent = 0f,
        )
    }
}

@Immutable
@JvmInline
private value class ScrollbarTrack(
    val packedValue: Long
) {
    constructor(
        max: Float,
        min: Float,
    ) : this(packFloats(max, min))
}

fun ScrollbarState(
    thumbSizePercent: Float,
    thumbTravelPercent: Float,
) = ScrollbarState(
    packFloats(
        val1 = thumbSizePercent,
        val2 = thumbTravelPercent
    )
)

val ScrollbarState.thumbSizePercent
    get() = unpackFloat1(packedValue)

val ScrollbarState.thumbTravelPercent
    get() = unpackFloat2(packedValue)

private val ScrollbarTrack.size
    get() = unpackFloat2(packedValue) - unpackFloat1(packedValue)

internal fun Orientation.coordinateValue(offset: Offset) = when (this) {
    Orientation.Horizontal -> offset.x
    Orientation.Vertical -> offset.y
}

internal fun Orientation.dimension(intSize: IntSize) = when (this) {
    Orientation.Horizontal -> intSize.width
    Orientation.Vertical -> intSize.height
}

internal fun Orientation.dimension(intOffset: IntOffset) = when (this) {
    Orientation.Horizontal -> intOffset.x
    Orientation.Vertical -> intOffset.y
}

@Composable
fun VerticalScrollbar(
    modifier: Modifier = Modifier,
    state: ScrollbarState,
    interactionSource: MutableInteractionSource? = null,
    thumb: @Composable () -> Unit,
    onThumbMoved: (Float) -> Unit,
) = Scrollbar(
    modifier = modifier,
    state = state,
    thumb = thumb,
    interactionSource = interactionSource,
    onThumbMoved = onThumbMoved,
    orientation = Orientation.Vertical,
)

@Composable
fun HorizontalScrollbar(
    modifier: Modifier = Modifier,
    state: ScrollbarState,
    interactionSource: MutableInteractionSource? = null,
    thumb: @Composable () -> Unit,
    onThumbMoved: (Float) -> Unit,
) = Scrollbar(
    modifier = modifier,
    state = state,
    thumb = thumb,
    interactionSource = interactionSource,
    onThumbMoved = onThumbMoved,
    orientation = Orientation.Horizontal,
)

@Composable
private fun Scrollbar(
    modifier: Modifier = Modifier,
    orientation: Orientation,
    state: ScrollbarState,
    minThumbSize: Dp = 40.dp,
    interactionSource: MutableInteractionSource? = null,
    thumb: @Composable () -> Unit,
    onThumbMoved: (Float) -> Unit,
) {
    val localDensity = LocalDensity.current
    var interactionThumbTravelPercent by remember { mutableStateOf(Float.NaN) }
    var pressedOffset by remember { mutableStateOf(Offset.Unspecified) }
    var draggedOffset by remember { mutableStateOf(Offset.Unspecified) }

    var track by remember { mutableStateOf(ScrollbarTrack(0)) }
    val updatedState by rememberUpdatedState(state)
    val updatedTrack by rememberUpdatedState(track)

    val thumbSizePercent = state.thumbSizePercent
    val thumbTravelPercent = when {
        interactionThumbTravelPercent.isNaN() -> state.thumbTravelPercent
        else -> interactionThumbTravelPercent
    }
    val thumbSizePx = max(
        a = thumbSizePercent * track.size,
        b = with(localDensity) { minThumbSize.toPx() }
    )

    val thumbSizeDp by animateDpAsState(
        targetValue = with(localDensity) { thumbSizePx.toDp() }
    )

    val thumbTravelPx = (track.size - thumbSizePx) * thumbTravelPercent

    val draggableState = rememberDraggableState { delta ->
        if (draggedOffset == Offset.Unspecified) return@rememberDraggableState

        draggedOffset = when (orientation) {
            Orientation.Vertical -> draggedOffset.copy(
                y = draggedOffset.y + delta
            )

            Orientation.Horizontal -> draggedOffset.copy(
                x = draggedOffset.x + delta
            )
        }
    }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .onGloballyPositioned { coordinates ->
                val position = orientation.coordinateValue(coordinates.positionInRoot())
                track = ScrollbarTrack(
                    max = position,
                    min = position + orientation.dimension(coordinates.size)
                )
            }
            // Process scrollbar presses
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val initialPress = PressInteraction.Press(offset)

                        interactionSource?.tryEmit(initialPress)
                        pressedOffset = offset

                        interactionSource?.tryEmit(
                            if (tryAwaitRelease()) PressInteraction.Release(initialPress)
                            else PressInteraction.Cancel(initialPress)
                        )
                        pressedOffset = Offset.Unspecified
                    },
                )
            }
            // Process scrollbar drags
            .draggable(
                state = draggableState,
                orientation = orientation,
                interactionSource = interactionSource,
                onDragStarted = { startedPosition: Offset ->
                    draggedOffset = startedPosition
                },
                onDragStopped = {
                    draggedOffset = Offset.Unspecified
                }
            )
    ) {
        val offset = max(
            a = with(localDensity) { thumbTravelPx.toDp() },
            b = 0.dp
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .run {
                    when (orientation) {
                        Orientation.Horizontal -> width(thumbSizeDp)
                        Orientation.Vertical -> height(thumbSizeDp)
                    }
                }
                .offset(
                    y = when (orientation) {
                        Orientation.Horizontal -> 0.dp
                        Orientation.Vertical -> offset
                    },
                    x = when (orientation) {
                        Orientation.Horizontal -> offset
                        Orientation.Vertical -> 0.dp
                    }
                )
                .run {
                    when (orientation) {
                        Orientation.Horizontal -> fillMaxHeight()
                        Orientation.Vertical -> fillMaxWidth()
                    }
                }
        ) {
            thumb()
        }
    }

    // Process presses
    LaunchedEffect(pressedOffset) {
        if (pressedOffset == Offset.Unspecified) {
            interactionThumbTravelPercent = Float.NaN
            return@LaunchedEffect
        }

        var currentTravel = updatedState.thumbTravelPercent
        val destinationTravel = updatedTrack.thumbPosition(
            dimension = orientation.coordinateValue(pressedOffset)
        )
        val isPositive = currentTravel < destinationTravel
        // TODO: Come up with a better heuristic for jumps
        val delta = if (isPositive) 0.1f else -0.1f

        while (currentTravel != destinationTravel) {
            currentTravel =
                if (isPositive) min(currentTravel + delta, destinationTravel)
                else max(currentTravel + delta, destinationTravel)
            onThumbMoved(currentTravel)
            interactionThumbTravelPercent = currentTravel
            // TODO: Define this more thoroughly
            delay(100)
        }
    }

    // Process drags
    LaunchedEffect(draggedOffset) {
        if (draggedOffset == Offset.Unspecified) {
            interactionThumbTravelPercent = Float.NaN
            return@LaunchedEffect
        }
        val currentTravel = updatedTrack.thumbPosition(
            dimension = orientation.coordinateValue(draggedOffset)
        )
        onThumbMoved(currentTravel)
        interactionThumbTravelPercent = currentTravel
    }
}

private fun ScrollbarTrack.thumbPosition(
    dimension: Float,
): Float = max(
    a = min(
        a = dimension / size,
        b = 1f
    ),
    b = 0f
)