package com.tunjid.me.scaffold.adaptive

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.intermediateLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun thumbnailSharedElementKey(
    property: Any?
) = "thumbnail-$property"

@Stable
internal class SharedElementData<T>(
    private val  key: Any,
    sharedElement: @Composable (T, Modifier) -> Unit,
    onRemoved: () -> Unit
) {
    private var inCount by mutableIntStateOf(0)

    val offsetAnimation = DeferredAnimation(
        vectorConverter = IntOffset.VectorConverter,
        animationSpec = sharedElementSpring()
    )
    val sizeAnimation = DeferredAnimation(
        vectorConverter = IntSize.VectorConverter,
        animationSpec = sharedElementSpring()
    )

    val moveableSharedElement: @Composable (Any?, Modifier) -> Unit =
        movableContentOf { state, modifier ->
            @Suppress("UNCHECKED_CAST")
            sharedElement(
                // The shared element composable will be created by the first screen and reused by
                // subsequent screens. This updates the state from other screens so changes are seen.
                state as T,
                Modifier
                    .sharedElement(
                        enabled = LocalAdaptiveContentScope.current.let {
                            it?.canAnimateSharedElements == true && it.isCurrentlyShared(key)
                        },
                        sharedElementData = this,
                    ) then modifier,
            )

            DisposableEffect(Unit) {
                ++inCount
                onDispose {
                    if (--inCount <= 0) onRemoved()
                }
            }
        }
}

/**
 * Allows a custom modifier to animate the local position and size of the layout within the
 * LookaheadLayout, whenever there's a change in the layout.
 */
internal fun Modifier.sharedElement(
    enabled: Boolean,
    sharedElementData: SharedElementData<*>,
): Modifier = composed {
    val coroutineScope = rememberCoroutineScope()
    // TODO: Optimize the not enabled path
    intermediateLayout { measurable, _ ->
        val (width, height) = sharedElementData.sizeAnimation.updateTarget(
            coroutineScope = coroutineScope,
            targetValue = lookaheadSize,
        )
        val animatedConstraints = Constraints.fixed(width, height)
        val placeable = measurable.measure(animatedConstraints)

        layout(placeable.width, placeable.height) layout@{
            val currentCoordinates = coordinates ?: return@layout placeable.place(x = 0, y = 0)
            val targetOffset = lookaheadScopeCoordinates.localLookaheadPositionOf(
                currentCoordinates
            )
            val animatedOffset = sharedElementData.offsetAnimation.updateTarget(
                coroutineScope,
                targetOffset.round(),
            )
            val currentOffset = lookaheadScopeCoordinates.localPositionOf(
                sourceCoordinates = currentCoordinates,
                relativeToSource = Offset.Zero
            ).round()

            val (x, y) = animatedOffset - currentOffset

            if (enabled) placeable.place(x = x, y = y)
            else placeable.place(x = 0, y = 0)
        }
    }
}

@Stable
internal class DeferredAnimation<T, V : AnimationVector>(
    private val vectorConverter: TwoWayConverter<T, V>,
    private val animationSpec: FiniteAnimationSpec<T>
) {
    val value: T? get() = animatable?.value ?: target

    private var target: T? by mutableStateOf(null)
    private var animatable: Animatable<T, V>? = null

    fun updateTarget(
        coroutineScope: CoroutineScope,
        targetValue: T,
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

private fun <T> sharedElementSpring() = spring<T>(stiffness = Spring.StiffnessLow)