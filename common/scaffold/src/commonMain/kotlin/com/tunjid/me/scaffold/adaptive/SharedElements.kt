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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.intermediateLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import com.tunjid.scaffold.adaptive.LocalAdaptiveContentScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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

    val offsetAnimation = DeferredAnimation(
        vectorConverter = IntOffset.VectorConverter,
    )
    val sizeAnimation = DeferredAnimation(
        vectorConverter = IntSize.VectorConverter,
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
): Modifier = this then composed {
    val sizeAnimComplete = sharedElementData.isComplete(
        SharedElementData<*>::sizeAnimation
    )
    val offsetAnimComplete = sharedElementData.isComplete(
        SharedElementData<*>::offsetAnimation
    )
    intermediateLayout { measurable, _ ->
        val (width, height) = sharedElementData.sizeAnimation.updateTarget(
            coroutineScope = this,
            target = lookaheadSize,
            animationSpec = sizeSpec,
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
                target = targetOffset.round(),
                animationSpec = offsetSpec,
            )

            if (sizeAnimComplete && offsetAnimComplete) return@layout placeable.place(
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
}

@Stable
internal class DeferredAnimation<T, V : AnimationVector>(
    private val vectorConverter: TwoWayConverter<T, V>,
) {
    /**
     * Returns the target value from the most recent [updateTarget] call.
     */
    val pendingTarget: T?
        get() = _pendingTarget

    private var _pendingTarget: T? by mutableStateOf(null)
    private val target: T?
        get() = animatable?.targetValue
    private var animatable: Animatable<T, V>? = null

    /**
     * [updateTarget] sets up an animation, or updates an already running animation, based on the
     * [target] in the given [coroutineScope]. [pendingTarget] will be updated to track the last
     * seen [target].
     *
     * [updateTarget] will return the current value of the animation after launching the animation
     * in the given [coroutineScope].
     *
     * @return current value of the animation
     */
    fun updateTarget(
        target: T,
        coroutineScope: CoroutineScope,
        animationSpec: FiniteAnimationSpec<T> = spring()
    ): T {
        _pendingTarget = target
        val anim = animatable ?: Animatable(target, vectorConverter).also { animatable = it }
        coroutineScope.launch {
            if (anim.targetValue != _pendingTarget) {
                anim.animateTo(target, animationSpec)
            }
        }
        return anim.value
    }

    /**
     * [isIdle] returns true when the animation has finished running and reached its
     * [pendingTarget], or when the animation has not been set up (i.e. [updateTarget] has never
     * been called).
     */
    val isIdle: Boolean
        get() = _pendingTarget == target && animatable?.isRunning != true
}

@Composable
private fun SharedElementData<*>.isComplete(
    animationMapper: (SharedElementData<*>) -> DeferredAnimation<*, *>
): Boolean {
    val animation = remember { animationMapper(this) }
    val adaptiveContentScope = LocalAdaptiveContentScope.current
    val scopeKey = adaptiveContentScope?.key
    var seenKeyCount by remember { mutableIntStateOf(0) }

    DisposableEffect(scopeKey) {
        seenKeyCount++
        onDispose { }
    }
    val animationCompleteAtLeastOnce by produceState(
        initialValue = false,
        key1 = scopeKey
    ) {
        value = false
        value = snapshotFlow { animation.isIdle }
            .filter(true::equals)
            .first()
    }
    return seenKeyCount < 1 || animationCompleteAtLeastOnce
}

private val sizeSpec = spring(
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntSize.VisibilityThreshold
)

private val offsetSpec = spring(
    stiffness = Spring.StiffnessMediumLow,
    dampingRatio = 0.9f,
    visibilityThreshold = IntOffset.VisibilityThreshold
)