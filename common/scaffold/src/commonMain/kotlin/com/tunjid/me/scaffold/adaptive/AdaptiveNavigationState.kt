package com.tunjid.scaffold.adaptive

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Stable
import com.tunjid.me.scaffold.adaptive.SharedElementScope
import com.tunjid.me.scaffold.globalui.WindowSizeClass
import com.tunjid.treenav.strings.Route

/**
 * Namespace for adaptive layout changes in an app
 */
object Adaptive {

    /**
     * Scope for adaptive content that can show up in an arbitrary [Container]
     */
    @Stable
    internal interface ContainerScope : AnimatedVisibilityScope, SharedElementScope {

        /**
         * Unique key to identify this scope
         */
        val key: String

        val containerState: ContainerState
    }

    /**
     * A layout in the hierarchy that hosts an [AdaptiveRouteConfiguration]
     */
    enum class Container {
        Primary, Secondary, TransientPrimary;

        companion object {
            internal val slots = Container.entries.indices.map(Adaptive::Slot)
        }
    }

    /**
     * A spot taken by an [AdaptiveRouteConfiguration] that may be moved in from [Container] to [Container]
     */
    @JvmInline
    value class Slot internal constructor(val index: Int)

    /**
     * Information about content in an [Adaptive.Container]
     */
    @Stable
    sealed interface ContainerState {
        val currentRoute: Route?
        val previousRoute: Route?
        val container: Container?
        val adaptation: Adaptation
    }

    internal val ContainerState.key get() = "${currentRoute?.id}-$container"

    /**
     * Describes how a route transitions after an adaptive change
     */
    data class Transitions(
        val enter: EnterTransition,
        val exit: ExitTransition,
    )

    /**
     * [Slot] based implementation of [ContainerState]
     */
    internal data class SlotContainerState(
        val slot: Slot?,
        override val currentRoute: Route?,
        override val previousRoute: Route?,
        override val container: Container?,
        override val adaptation: Adaptation,
    ) : ContainerState

    /**
     * A description of the process that the layout undertook to adapt to its new configuration
     */
    sealed class Adaptation {
        /**
         * Routes were changed in containers
         */
        data object Change : Adaptation()

        /**
         * Routes were swapped in between containers
         */
        data class Swap(
            val from: Container,
            val to: Container?,
        ) : Adaptation()

        operator fun Swap.contains(container: Container?) = container == from || container == to

        companion object {
            val PrimaryToSecondary = Swap(
                from = Container.Primary,
                to = Container.Secondary
            )

            val SecondaryToPrimary = Swap(
                from = Container.Secondary,
                to = Container.Primary
            )

            val PrimaryToTransient = Swap(
                from = Container.Primary,
                to = Container.TransientPrimary
            )
        }
    }

    interface NavigationState {

        val routeIds: Collection<String>

        val windowSizeClass: WindowSizeClass
        fun containerStateFor(
            slot: Slot
        ): ContainerState

        fun slotFor(
            container: Container?
        ): Slot?

        fun containerFor(
            route: Route
        ): Container?

        fun routeFor(
            slot: Slot
        ): Route?

        fun routeFor(
            container: Container
        ): Route?

        fun adaptationIn(
            container: Container
        ): Adaptation?
    }

}