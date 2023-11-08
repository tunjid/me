package com.tunjid.me.scaffold.globalui.scaffold

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.tunjid.me.scaffold.globalui.LocalGlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.isFromLeft
import com.tunjid.me.scaffold.globalui.progress
import com.tunjid.me.scaffold.globalui.touchX
import com.tunjid.me.scaffold.globalui.touchY
import com.tunjid.me.scaffold.lifecycle.mappedCollectAsStateWithLifecycle
import kotlin.math.roundToInt

// Previews back content as specified by the material motion spec for Android predictive back:
// https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back#motion-specs
internal actual fun Modifier.backPreviewModifier(): Modifier =
    this then Modifier.composed {
        val configuration = LocalConfiguration.current
        val globalUiStateHolder = LocalGlobalUiStateHolder.current

        val uiStateFlow = remember {
            globalUiStateHolder.state
        }
        val backStatus by uiStateFlow.mappedCollectAsStateWithLifecycle(
            mapper = UiState::backStatus
        )
        val scale by animateFloatAsState(
            // Deviates from the spec here. The spec says 90% of the container, I'm doing 85%
            targetValue = 1f - (backStatus.progress * 0.15F),
            label = "back preview modifier scale"
        )
        var elevation by remember { mutableStateOf(0.dp) }
        LaunchedEffect(Unit) {
            animate(
                initialValue = 0f,
                targetValue = 4f,
                animationSpec = spring(stiffness = Spring.StiffnessVeryLow)
            ) { value, _ -> elevation = value.dp }
        }

        Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    constraints.copy(
                        maxWidth = (constraints.maxWidth * scale).roundToInt(),
                        minWidth = (constraints.minWidth * scale).roundToInt(),
                        maxHeight = (constraints.maxHeight * scale).roundToInt(),
                        minHeight = (constraints.minHeight * scale).roundToInt(),
                    )
                )
                val containerWidth = placeable.width
                val containerHeight = placeable.height

                val scaledWidth = containerWidth * scale
                val spaceOnEachSide = (containerWidth - scaledWidth) / 2
                val margin = (BACK_PREVIEW_PADDING * backStatus.progress).dp.roundToPx()

                val xOffset = ((spaceOnEachSide - margin) * when {
                    backStatus.isFromLeft -> 1
                    else -> -1
                }).toInt()

                val maxYShift = ((containerHeight / 20) - BACK_PREVIEW_PADDING)
                val isOrientedHorizontally = containerWidth > containerHeight
                val screenSize = when {
                    isOrientedHorizontally -> configuration.screenWidthDp
                    else -> configuration.screenHeightDp
                }.dp.roundToPx()
                val touchPoint = when {
                    isOrientedHorizontally -> backStatus.touchX
                    else -> backStatus.touchY
                }
                val verticalProgress = (touchPoint / screenSize) - 0.5f
                val yOffset = (verticalProgress * maxYShift).roundToInt()

                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(x = xOffset, y = yOffset)
                }
            }
            .background(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation),
                shape = RoundedCornerShape(16.dp)
            )
            // Disable interactions in the preview container
            .pointerInput(Unit) {}
    }

private const val BACK_PREVIEW_PADDING = 8
