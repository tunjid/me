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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
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
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import kotlin.math.max
import kotlin.math.min

@Immutable
@JvmInline
value class ScrollbarState internal constructor(internal val packedValue: Long) {
    companion object {
        val FULL = ScrollbarState(
            thumbHeightPercent = 1f,
            thumbTravelPercent = 0f,
        )
    }
}

val ScrollbarState.thumbHeightPercent get() = unpackFloat1(packedValue)

val ScrollbarState.thumbTravelPercent get() = unpackFloat2(packedValue)

var recr = 0
@Composable
fun Scrollbar(
    modifier: Modifier = Modifier,
    state: ScrollbarState,
    thumb: @Composable () -> Unit,
    onThumbMoved: (Float) -> Unit,
) {
    val localDensity = LocalDensity.current
    var minY by remember { mutableStateOf(0f) }
    var maxY by remember { mutableStateOf(0f) }

    val thumbHeightPercent = state.thumbHeightPercent
    val thumbTravelPercent = state.thumbTravelPercent
    val thumbHeightPx = thumbHeightPx(thumbHeightPercent, maxY, minY)

    val thumbHeightDp by animateDpAsState(
        with(localDensity) { thumbHeightPx.toDp() }
    )
    val thumbTravelPx by animateIntAsState(
        targetValue = min(
            a = ((maxY - minY) * thumbTravelPercent).toInt(),
            b = ((maxY - minY) - thumbHeightPx).toInt()
        ),
        animationSpec = SpringSpec(
            stiffness = Spring.StiffnessHigh
        )
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .onGloballyPositioned {
                minY = it.positionInRoot().y
                maxY = minY + it.size.height
            }
            .pointerInput(minY, maxY) {
                detectTapGestures {
                    onThumbMoved(
                        it.y.calculateThumbMove(
                            maxY = maxY,
                            minY = minY
                        )
                    )
                }
            }
            .pointerInput(minY, maxY) {
                detectDragGestures { change, _ ->
                    onThumbMoved(
                        change.position.y.calculateThumbMove(
                            maxY = maxY,
                            minY = minY
                        )
                    )
                }
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .height(thumbHeightDp)
                .offset(y = with(localDensity) { thumbTravelPx.toDp() })
                .fillMaxWidth()
        ) {
            thumb()
        }
    }
}

private fun thumbHeightPx(
    thumbHeightPercent: Float,
    maxY: Float,
    minY: Float,
): Float = max(
    a = thumbHeightPercent,
    b = 0.1f
) * (maxY - minY)

private fun Float.calculateThumbMove(
    maxY: Float,
    minY: Float,
): Float = max(
    a = min(
        a = this / (maxY - minY),
        b = 1f
    ),
    b = 0f
)

fun ScrollbarState(
    thumbHeightPercent: Float,
    thumbTravelPercent: Float,
) = ScrollbarState(
    packFloats(
        val1 = thumbHeightPercent,
        val2 = thumbTravelPercent
    )
)

fun ScrollbarState(
    available: Int,
    visible: Int,
    index: Int,
): ScrollbarState =
    when {
        available != 0 -> ScrollbarState(
            thumbHeightPercent = visible.toFloat() / available,
            thumbTravelPercent = index.toFloat() / available
        )

        else -> ScrollbarState(
            thumbHeightPercent = 1f,
            thumbTravelPercent = 0f,
        )
    }