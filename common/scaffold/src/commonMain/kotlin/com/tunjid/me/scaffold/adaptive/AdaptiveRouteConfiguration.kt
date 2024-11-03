package com.tunjid.scaffold.adaptive

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import com.tunjid.me.scaffold.navigation.SerializedRouteParams
import com.tunjid.treenav.Node
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParams
import kotlinx.serialization.Serializable

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

/**
 * [AdaptiveRouteConfiguration] instances with no state holder
 */
abstract class StatelessRoute : Route

/**
 * Basic route definition
 */
@Serializable
private class BasicRoute(
    override val routeParams: SerializedRouteParams,
    override val children: List<Node> = emptyList(),
) : Route {

    constructor(
        path: String,
        pathArgs: Map<String, String> = emptyMap(),
        queryParams: Map<String, List<String>> = emptyMap(),
        children: List<Node> = emptyList(),
    ) : this(
        routeParams = RouteParams(
            pathAndQueries = path,
            pathArgs = pathArgs,
            queryParams = queryParams,
        ),
        children = children
    )

    override fun toString(): String = id
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Route) return false

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

fun routeOf(
    params: RouteParams,
    children: List<Node> = emptyList(),
): Route = BasicRoute(
    routeParams = params,
    children = children
)

fun routeOf(
    path: String,
    pathArgs: Map<String, String> = emptyMap(),
    queryParams: Map<String, List<String>> = emptyMap(),
    children: List<Node> = listOf(),
): Route = BasicRoute(
    path = path,
    pathArgs = pathArgs,
    queryParams = queryParams,
    children = children
)

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