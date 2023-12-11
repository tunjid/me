package com.tunjid.me.scaffold.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import com.tunjid.me.scaffold.adaptive.Adaptive
import com.tunjid.treenav.strings.Route

/**
 * Route implementation with adaptive semantics
 */
interface AdaptiveRoute : Route {

    override val id get() = routeParams.route.split("?").first()

    @Composable
    fun content()

    /**
     * Defines what route to show in the secondary panel alongside this route
     */
    val secondaryRoute: String?
        get() = null

    fun transitionsFor(
        state: Adaptive.ContainerState
    ): Adaptive.Transitions = when (state.container) {
        Adaptive.Container.Primary,
        Adaptive.Container.Secondary -> when (state.adaptation) {
            Adaptive.Adaptation.PrimaryToSecondary,
            Adaptive.Adaptation.SecondaryToPrimary -> NoTransition
            else -> DefaultTransition
        }

        Adaptive.Container.TransientPrimary -> when(state.adaptation) {
            Adaptive.Adaptation.PrimaryToTransient -> when (state.container) {
                Adaptive.Container.Secondary -> DefaultTransition
                else -> Adaptive.Transitions(
                    enter = EnterTransition.None,
                    exit = ExitTransition.None,
                )
            }
            else -> DefaultTransition
        }
        null -> NoTransition
    }
}

private val RouteTransitionAnimationSpec: FiniteAnimationSpec<Float> = tween(
    durationMillis = 700
)

private val DefaultTransition = Adaptive.Transitions(
    enter = fadeIn(
        animationSpec = RouteTransitionAnimationSpec,
        // This is needed because I can't exclude shared elements from transitions
        // so to actually see them move, state fading in from 0.1f
        initialAlpha = 0.1f
    ),
    exit = fadeOut(
        animationSpec = RouteTransitionAnimationSpec
    )
)

private val NoTransition = Adaptive.Transitions(
    enter = EnterTransition.None,
    exit = ExitTransition.None,
)