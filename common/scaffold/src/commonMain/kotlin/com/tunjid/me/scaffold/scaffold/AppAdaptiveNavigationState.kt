package com.tunjid.me.scaffold.scaffold

import androidx.compose.runtime.Immutable
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.WindowSizeClass
import com.tunjid.me.scaffold.globalui.slices.RouteContainerPositionalState
import com.tunjid.me.scaffold.globalui.slices.routeContainerState
import com.tunjid.me.scaffold.navigation.UnknownRoute
import com.tunjid.scaffold.adaptive.Adaptive
import com.tunjid.scaffold.adaptive.Adaptive.Adaptation.Change.contains
import com.tunjid.treenav.strings.Route

/**
 * Data structure for managing navigation as it adapts to various layout configurations
 */
@Immutable
internal data class AppAdaptiveNavigationState(
    /**
     * Moves between containers within a navigation sequence.
     */
    val swapAdaptations: Set<Adaptive.Adaptation.Swap>,
    /**
     * A mapping of [Container] to the routes in them
     */
    val containersToRoutes: Map<Adaptive.Container, Route?>,
    /**
     * A mapping of route ids to the adaptive slots they are currently in.
     */
    val routeIdsToAdaptiveSlots: Map<String?, Adaptive.Slot>,
    /**
     * A mapping of adaptive container to the routes that were last in them.
     */
    val previousContainersToRoutes: Map<Adaptive.Container, Route?>,
    /**
     * A set of route ids that may be returned to.
     */
    val backStackIds: Set<String>,
    /**
     * A set of route ids that are animating out.
     */
    val routeIdsAnimatingOut: Set<String>,
    /**
     * The window size class of the current screen configuration
     */
    override val windowSizeClass: WindowSizeClass,
    /**
     * The positionalState of route containers
     */
    val routeContainerPositionalState: RouteContainerPositionalState,
) : Adaptive.NavigationState {
    companion object {
        internal val Initial = AppAdaptiveNavigationState(
            swapAdaptations = emptySet(),
            windowSizeClass = WindowSizeClass.COMPACT,
            containersToRoutes = mapOf(
                Adaptive.Container.Primary to UnknownRoute(
                    Adaptive.Container.slots.first().toString()
                )
            ),
            routeIdsToAdaptiveSlots = Adaptive.Container.slots.associateBy(Adaptive.Slot::toString),
            backStackIds = emptySet(),
            routeIdsAnimatingOut = emptySet(),
            previousContainersToRoutes = emptyMap(),
            routeContainerPositionalState = UiState().routeContainerState,
        )
    }

    override val routeIds: Collection<String>
        get() = backStackIds

    override fun containerStateFor(
        slot: Adaptive.Slot
    ): Adaptive.ContainerState {
        val route = routeFor(slot)
        val container = route?.let(::containerFor)
        return Adaptive.SlotContainerState(
            slot = slot,
            currentRoute = route,
            previousRoute = previousContainersToRoutes[container],
            container = container,
            adaptation = swapAdaptations.firstOrNull { container in it }
                ?: Adaptive.Adaptation.Change,
        )
    }

    override fun slotFor(
        container: Adaptive.Container?
    ): Adaptive.Slot? = when (container) {
        null -> null
        else -> routeIdsToAdaptiveSlots[containersToRoutes[container]?.id]
    }

    override fun containerFor(
        route: Route
    ): Adaptive.Container? =
        containersToRoutes.firstNotNullOfOrNull { (container, containerRoute) ->
            if (containerRoute?.id == route.id) container else null
        }

    override fun routeFor(
        slot: Adaptive.Slot
    ): Route? = routeIdsToAdaptiveSlots.firstNotNullOfOrNull { (routeId, routeSlot) ->
        if (routeSlot == slot) containersToRoutes.firstNotNullOfOrNull { (_, route) ->
            if (route?.id == routeId) route
            else null
        }
        else null
    }

    override fun routeFor(
        container: Adaptive.Container
    ): Route? = containersToRoutes[container]

    override fun adaptationIn(
        container: Adaptive.Container
    ): Adaptive.Adaptation? = swapAdaptations.firstOrNull { container in it }
}