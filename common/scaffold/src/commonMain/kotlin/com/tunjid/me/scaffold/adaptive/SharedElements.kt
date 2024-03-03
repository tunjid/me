package com.tunjid.me.scaffold.adaptive

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.intermediateLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import com.tunjid.scaffold.adaptive.Adaptive
import com.tunjid.scaffold.adaptive.LocalAdaptiveContentScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal interface SharedElementScope {
    fun isCurrentlyShared(key: Any): Boolean

    @Composable
    fun <T> sharedElementOf(
        key: Any,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit
}

fun thumbnailSharedElementKey(
    property: Any?
) = "thumbnail-$property"

@Stable
internal class SharedElementData<T>(
    sharedElement: @Composable (T, Modifier) -> Unit,
    onRemoved: () -> Unit
) {
    private var inCount by mutableIntStateOf(0)
    var canAnimate by mutableStateOf(true)

    val offsetAnimation = DeferredAnimation(
        vectorConverter = IntOffset.VectorConverter,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = 0.9f,
            visibilityThreshold = IntOffset.VisibilityThreshold
        )
    )
    val sizeAnimation = DeferredAnimation(
        vectorConverter = IntSize.VectorConverter,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntSize.VisibilityThreshold
        )
    )

    val moveableSharedElement: @Composable (Any?, Modifier) -> Unit =
        movableContentOf { state, modifier ->
            val scope = LocalAdaptiveContentScope.current
            SideEffect {
                when (scope?.containerState?.container) {
                    Adaptive.Container.Primary -> canAnimate = scope.transition.isRunning
                    Adaptive.Container.Secondary -> canAnimate = scope.transition.isRunning
                    Adaptive.Container.TransientPrimary -> canAnimate = false
                    null -> Unit
                }
            }

            @Suppress("UNCHECKED_CAST")
            sharedElement(
                // The shared element composable will be created by the first screen and reused by
                // subsequent screens. This updates the state from other screens so changes are seen.
                state as T,
                Modifier
                    .sharedElement(
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
    sharedElementData: SharedElementData<*>,
): Modifier = this then intermediateLayout { measurable, _ ->
    val (width, height) = sharedElementData.sizeAnimation.updateTarget(
        coroutineScope = this,
        targetValue = lookaheadSize,
    )
    val animatedConstraints = Constraints.fixed(width, height)
    val placeable = measurable.measure(animatedConstraints)

    layout(placeable.width, placeable.height) layout@{
        val currentCoordinates = coordinates ?: return@layout placeable.place(
            x = 0,
            y = 0
        )
        val targetOffset = lookaheadScopeCoordinates.localLookaheadPositionOf(
            currentCoordinates
        )
        val animatedOffset = sharedElementData.offsetAnimation.updateTarget(
            coroutineScope = this@intermediateLayout,
            targetValue = targetOffset.round(),
        )

        if (!sharedElementData.canAnimate) return@layout placeable.place(
            x = 0,
            y = 0
        )

        val currentOffset = lookaheadScopeCoordinates.localPositionOf(
            sourceCoordinates = currentCoordinates,
            relativeToSource = Offset.Zero
        ).round()

        val (x, y) = animatedOffset - currentOffset
        placeable.place(
            x = x,
            y = y
        )
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
