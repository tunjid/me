/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tunjid.composables.scrollbars.Scrollbar
import com.tunjid.composables.scrollbars.ScrollbarState
import kotlinx.coroutines.delay

/**
 * A [Scrollbar] that allows for fast scrolling of content.
 * Its thumb disappears when the scrolling container is dormant.
 * @param modifier a [Modifier] for the [Scrollbar]
 * @param state the driving state for the [Scrollbar]
 * @param scrollInProgress a flag indicating if the scrolling container for the scrollbar is
 * currently scrolling
 * @param orientation the orientation of the scrollbar
 * @param onThumbMoved the fast scroll implementation
 */
@Composable
fun FastScrollbar(
    modifier: Modifier = Modifier,
    state: ScrollbarState,
    scrollInProgress: Boolean,
    orientation: Orientation,
    onThumbMoved: (Float) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Scrollbar(
        modifier = modifier,
        orientation = orientation,
        interactionSource = interactionSource,
        state = state,
        thumb = {
            FastScrollbarThumb(
                scrollInProgress = scrollInProgress,
                interactionSource = interactionSource,
                orientation = orientation,
            )
        },
        onThumbMoved = onThumbMoved,
    )
}

/**
 * A scrollbar thumb that is intended to also be a touch target for fast scrolling.
 */
@Composable
private fun FastScrollbarThumb(
    scrollInProgress: Boolean,
    interactionSource: InteractionSource,
    orientation: Orientation,
) {
    Box(
        modifier = Modifier
            .run {
                when (orientation) {
                    Vertical -> width(12.dp).fillMaxHeight()
                    Horizontal -> height(12.dp).fillMaxWidth()
                }
            }
            .background(
                color = scrollbarThumbColor(
                    scrollInProgress = scrollInProgress,
                    interactionSource = interactionSource,
                ),
                shape = RoundedCornerShape(16.dp),
            ),
    )
}

/**
 * The color of the scrollbar thumb as a function of its interaction state.
 * @param scrollInProgress if the scrolling container is currently scrolling
 * @param interactionSource source of interactions in the scrolling container
 */
@Composable
private fun scrollbarThumbColor(
    scrollInProgress: Boolean,
    interactionSource: InteractionSource,
): Color {
    var state by remember { mutableStateOf(ThumbState.Active) }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val active = pressed || hovered || dragged || scrollInProgress

    val color by animateColorAsState(
        targetValue = when (state) {
            ThumbState.Active -> MaterialTheme.colorScheme.tertiaryContainer
            ThumbState.Inactive -> MaterialTheme.colorScheme.surfaceColorAtElevation(16.dp)
            ThumbState.Dormant -> Color.Transparent
        },
        animationSpec = SpringSpec(
            stiffness = Spring.StiffnessLow,
        ),
        label = "Scrollbar thumb color",
    )
    LaunchedEffect(active) {
        when (active) {
            true -> state = ThumbState.Active
            false -> {
                state = ThumbState.Inactive
                delay(2_000)
                state = ThumbState.Dormant
            }
        }
    }

    return color
}

private enum class ThumbState {
    Active, Inactive, Dormant
}
