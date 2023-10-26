package com.tunjid.me.scaffold.globalui.adaptive

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Placeable.PlacementScope
import androidx.compose.ui.layout.intermediateLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
internal class SharedElementData(
    val lookaheadScope: LookaheadScope
) {
    val offsetAnimation = DeferredAnimation(
        IntOffset.VectorConverter
    )
    val sizeAnimation = DeferredAnimation(
        IntSize.VectorConverter
    )

    var placementOffset: IntOffset by mutableStateOf(IntOffset.Zero)
}

/**
 * Allows a custom modifier to animate the local position and size of the layout within the
 * LookaheadLayout, whenever there's a change in the layout.
 */
internal fun Modifier.sharedElement(
    enabled: Boolean,
    sharedElementData: SharedElementData,
): Modifier = composed {
    val coroutineScope = rememberCoroutineScope()
    intermediateLayout { measurable, _ ->
        val (width, height) = sharedElementData.sizeAnimation.updateTarget(
            coroutineScope = coroutineScope,
            targetValue = lookaheadSize,
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        )
        val animatedConstraints = Constraints.fixed(width, height)
        val placeable = measurable.measure(animatedConstraints)

        layout(placeable.width, placeable.height) {
            val (x, y) = sharedElementData.offsetAnimation.updateTargetBasedOnCoordinates(
                placementScope = this,
                coroutineScope = coroutineScope,
                lookaheadScope = sharedElementData.lookaheadScope,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
            coordinates?.let {
                sharedElementData.placementOffset = lookaheadScopeCoordinates.localPositionOf(
                    sourceCoordinates = it,
                    relativeToSource = Offset.Zero
                ).round()
            }
            if (enabled) placeable.place(x = x, y = y)
            else placeable.place(0, 0)
        }
    }
}

internal class DeferredAnimation<T, V : AnimationVector>(
    private val vectorConverter: TwoWayConverter<T, V>
) {
    val value: T? get() = animatable?.value ?: target

    private var target: T? by mutableStateOf(null)
    private var animatable: Animatable<T, V>? = null

    fun updateTarget(
        coroutineScope: CoroutineScope,
        targetValue: T,
        animationSpec: FiniteAnimationSpec<T>
    ): T = with(coroutineScope) {
        target = targetValue
        if (target != null && target != animatable?.targetValue) {
            when (val currentAnimatable = animatable) {
                null -> animatable = Animatable(targetValue, vectorConverter)
                else -> launch {
                    currentAnimatable.animateTo(
                        targetValue = targetValue,
                        animationSpec = animationSpec
                    )
                }
            }
        }
        return animatable?.value ?: targetValue
    }
}

private fun DeferredAnimation<IntOffset, AnimationVector2D>.updateTargetBasedOnCoordinates(
    placementScope: PlacementScope,
    lookaheadScope: LookaheadScope,
    coroutineScope: CoroutineScope,
    animationSpec: FiniteAnimationSpec<IntOffset>,
): IntOffset = with(lookaheadScope) {
    when (val coordinates = placementScope.coordinates) {
        null -> IntOffset.Zero
        else -> with(placementScope) {
            val targetOffset = lookaheadScopeCoordinates.localLookaheadPositionOf(coordinates)
            val animOffset = updateTarget(
                coroutineScope,
                targetOffset.round(),
                animationSpec,
            )
            val current = lookaheadScopeCoordinates.localPositionOf(
                sourceCoordinates = coordinates,
                relativeToSource = Offset.Zero
            ).round()
            (animOffset - current)
        }
    }
}
