package com.tunjid.scaffold.adaptive

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParams

/**
 * Route implementation with adaptive semantics
 */
interface AdaptiveRouteConfiguration {

    @Composable
    fun Render(route: Route)

    fun transitionsFor(
        state: Adaptive.ContainerState
    ): Adaptive.Transitions = NoTransition

    /**
     * Defines what route to show in the secondary panel alongside this route
     */
    fun secondaryRoute(route: Route): Route?
}

fun adaptiveRouteConfiguration(
    secondaryRoute: (Route) -> ExternalRoute? = { null },
    transitions: (Adaptive.ContainerState) -> Adaptive.Transitions = { state ->
        when (state.container) {
            Adaptive.Container.Primary,
            Adaptive.Container.Secondary -> when (state.adaptation) {
                Adaptive.Adaptation.PrimaryToSecondary,
                Adaptive.Adaptation.SecondaryToPrimary -> NoTransition

                else -> DefaultTransition
            }

            Adaptive.Container.TransientPrimary -> when (state.adaptation) {
                Adaptive.Adaptation.PrimaryToTransient -> when (state.container) {
                    Adaptive.Container.Secondary -> DefaultTransition
                    else -> NoTransition
                }

                else -> DefaultTransition
            }

            null -> NoTransition
        }
    },
    render: @Composable (Route) -> Unit
) = object : AdaptiveRouteConfiguration {

    @Composable
    override fun Render(route: Route) {
        render(route)
    }

    override fun secondaryRoute(route: Route): Route? =
        secondaryRoute(route)

    override fun transitionsFor(state: Adaptive.ContainerState): Adaptive.Transitions =
        transitions(state)
}

/**
 * [AdaptiveRouteConfiguration] instances with no state holder
 */
abstract class StatelessRoute : Route

/**
 * A route that has a id for a [Route] defined in another module
 */
class ExternalRoute(
    val path: String,
) : StatelessRoute() {
    override val routeParams: RouteParams
        get() = RouteParams(
            pathAndQueries = path,
            pathArgs = emptyMap(),
            queryParams = emptyMap(),
        )
}

private val RouteTransitionAnimationSpec: FiniteAnimationSpec<Float> = tween(
    durationMillis = 700
)

private val DefaultTransition = Adaptive.Transitions(
    enter = fadeIn(
        animationSpec = RouteTransitionAnimationSpec,
        // This is needed because I can't exclude shared elements from transitions
        // so to actually see them move, start fading in from 0.1f
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