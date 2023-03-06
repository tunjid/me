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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import kotlin.math.max
import kotlin.math.min

@Immutable
@JvmInline
value class ScrollbarState internal constructor(internal val packedValue: Long) {
    companion object {
        val FULL = scrollbarState(
            thumbHeightPercent = 1f,
            thumbTravelPercent = 0f,
        )
    }
}

val ScrollbarState.thumbHeightPercent get() = unpackFloat1(packedValue)

val ScrollbarState.thumbTravelPercent get() = unpackFloat2(packedValue)

@Immutable
@JvmInline
private value class ScrollbarTrack(val packedValue: Long) {
    constructor(
        top: Float,
        bottom: Float,
    ) : this(packFloats(top, bottom))
}

private val ScrollbarTrack.height
    get() =
        unpackFloat2(packedValue) - unpackFloat1(packedValue)

@Composable
fun Scrollbar(
    modifier: Modifier = Modifier,
    state: ScrollbarState,
    thumb: @Composable (isActive: Boolean) -> Unit,
    onThumbMoved: (Float) -> Unit,
) {
    val localDensity = LocalDensity.current
    var isActive by remember { mutableStateOf(false) }
    var track by remember { mutableStateOf(ScrollbarTrack(0)) }

    val thumbHeightPercent = state.thumbHeightPercent
    val thumbTravelPercent = state.thumbTravelPercent
    val thumbHeightPx = thumbHeightPx(thumbHeightPercent, track.height)

    val thumbHeightDp by animateDpAsState(
        with(localDensity) { thumbHeightPx.toDp() }
    )

    val thumbTravelPx = min(
        a = (track.height * thumbTravelPercent).toInt(),
        b = (track.height - thumbHeightPx).toInt()
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .onGloballyPositioned {
                track = ScrollbarTrack(
                    top = it.positionInRoot().y,
                    bottom = it.positionInRoot().y + it.size.height
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
                            track.thumbPosition(y = offset.y)
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
                            track.thumbPosition(y = change.position.y)
                        )
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .height(thumbHeightDp)
                .offset(
                    y = max(
                        a = with(localDensity) { thumbTravelPx.toDp() },
                        b = 0.dp
                    )
                )
                .fillMaxWidth()
        ) {
            thumb(isActive)
        }
    }
}

private fun thumbHeightPx(
    thumbHeightPercent: Float,
    trackHeight: Float,
): Float = max(
    a = thumbHeightPercent,
    b = 0.1f
) * (trackHeight)

private fun ScrollbarTrack.thumbPosition(
    y: Float,
): Float = max(
    a = min(
        a = y / height,
        b = 1f
    ),
    b = 0f
)

fun scrollbarState(
    thumbHeightPercent: Float,
    thumbTravelPercent: Float,
) = ScrollbarState(
    packFloats(
        val1 = thumbHeightPercent,
        val2 = thumbTravelPercent
    )
)

fun scrollbarState(
    available: Int,
    visible: Int,
    index: Int,
): ScrollbarState =
    when {
        available != 0 -> scrollbarState(
            thumbHeightPercent = visible.toFloat() / available,
            thumbTravelPercent = index.toFloat() / available
        )

        else -> scrollbarState(
            thumbHeightPercent = 1f,
            thumbTravelPercent = 0f,
        )
    }