package com.tunjid.me.scaffold.globalui.scaffold

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val globalUiStateHolder = LocalGlobalUiStateHolder.current

        val uiStateFlow = remember {
            globalUiStateHolder.state
        }
        val backStatus by uiStateFlow.mappedCollectAsStateWithLifecycle(
            mapper = UiState::backStatus
        )
        val scale by animateFloatAsState(
            // Deviates from the spec here. The spec says 90% of the container, I'm doing 80%
            targetValue = 1f - (backStatus.progress * 0.2F),
            label = "back preview modifier scale"
        )
        val color = MaterialTheme.colorScheme.surface

        Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val containerWidth = placeable.width
                val containerHeight = placeable.height

                val maxXShift = ((containerWidth / 20) - BACK_PREVIEW_PADDING)
                val maxYShift = ((containerHeight / 20) - BACK_PREVIEW_PADDING)
                val isOrientedHorizontally = maxXShift > maxYShift

                val xOffset =
                    (backStatus.progress * maxXShift).roundToInt() * (if (backStatus.isFromLeft) 1 else -1)

                val touchPoint =
                    if (isOrientedHorizontally) backStatus.touchX
                    else backStatus.touchY

                val screenSize = with(density) {
                    (if (isOrientedHorizontally) configuration.screenWidthDp
                    else configuration.screenHeightDp).dp.toPx()
                }
                val verticalProgress = (touchPoint / screenSize) - 0.5f

                val yOffset = (verticalProgress * maxYShift).roundToInt()

                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(x = xOffset, y = yOffset)
                }
            }
            .padding(
                start =
                if (backStatus.isFromLeft) 0.dp
                else (BACK_PREVIEW_PADDING * backStatus.progress).dp,
                end =
                if (backStatus.isFromLeft) (BACK_PREVIEW_PADDING * backStatus.progress).dp
                else 0.dp
            )
            .scale(scale)
            .background(
                color = color,
                shape = RoundedCornerShape(16.dp)
            )
            // Disable interactions in the preview container
            .pointerInput(Unit) {}
    }

private const val BACK_PREVIEW_PADDING = 8