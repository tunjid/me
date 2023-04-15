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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import kotlin.math.max
import kotlin.math.min

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

private val ScrollbarState.thumbSizePercent
    get() = unpackFloat1(packedValue)

private val ScrollbarState.thumbTravelPercent
    get() = unpackFloat2(packedValue)

private val ScrollbarTrack.size
    get() = unpackFloat2(packedValue) - unpackFloat1(packedValue)

fun Offset.dimension(orientation: Orientation) = when (orientation) {
    Orientation.Horizontal -> x
    Orientation.Vertical -> y
}

fun IntSize.dimension(orientation: Orientation) = when (orientation) {
    Orientation.Horizontal -> width
    Orientation.Vertical -> height
}

@Composable
fun VerticalScrollbar(
    modifier: Modifier = Modifier,
    state: ScrollbarState,
    thumb: @Composable (isActive: Boolean) -> Unit,
    onThumbMoved: (Float) -> Unit,
) = Scrollbar(
    modifier = modifier,
    state = state,
    thumb = thumb,
    onThumbMoved = onThumbMoved,
    orientation = Orientation.Vertical,
)

@Composable
fun HorizontalScrollbar(
    modifier: Modifier = Modifier,
    state: ScrollbarState,
    thumb: @Composable (isActive: Boolean) -> Unit,
    onThumbMoved: (Float) -> Unit,
) = Scrollbar(
    modifier = modifier,
    state = state,
    thumb = thumb,
    onThumbMoved = onThumbMoved,
    orientation = Orientation.Horizontal,
)

@Composable
private fun Scrollbar(
    modifier: Modifier = Modifier,
    orientation: Orientation,
    state: ScrollbarState,
    minThumbSize: Dp = 40.dp,
    thumb: @Composable (isActive: Boolean) -> Unit,
    onThumbMoved: (Float) -> Unit,
) {
    val localDensity = LocalDensity.current
    var isActive by remember { mutableStateOf(false) }
    var track by remember { mutableStateOf(ScrollbarTrack(0)) }

    val thumbSizePercent = state.thumbSizePercent
    val thumbTravelPercent = state.thumbTravelPercent
    val thumbSizePx = max(
        a = thumbSizePercent * track.size,
        b = with(localDensity) { minThumbSize.toPx() }
    )

    val thumbSizeDp by animateDpAsState(
        with(localDensity) { thumbSizePx.toDp() }
    )

    val thumbTravelPx by animateFloatAsState(
        min(
            a = track.size * thumbTravelPercent,
            b = track.size - thumbSizePx
        )
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .onGloballyPositioned {
                val position = it.positionInRoot().dimension(orientation)
                track = ScrollbarTrack(
                    max = position,
                    min = position + it.size.dimension(orientation)
                )
            }
            .pointerInput(track) {
                detectTapGestures(
                    onPress = {
                        isActive = true
                        tryAwaitRelease()
                        isActive = false
                    },
                    onDoubleTap = {
                        isActive = false
                    },
                    onTap = { offset ->
                        isActive = false
                        onThumbMoved(
                            track.thumbPosition(dimension = offset.dimension(orientation))
                        )
                    })
            }
            .pointerInput(track) {
                detectDragGestures(
                    onDragStart = { isActive = true },
                    onDragEnd = { isActive = false },
                    onDrag = { change, _ ->
                        isActive = true
                        onThumbMoved(
                            track.thumbPosition(dimension = change.position.dimension(orientation))
                        )
                    }
                )
            }
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
            thumb(isActive)
        }
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
