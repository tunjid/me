/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.me.scaffold.globalui.adaptive

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.WindowSizeClass
import com.tunjid.me.scaffold.globalui.slices.RouteContainerPositionalState
import com.tunjid.me.scaffold.globalui.slices.routeContainerState
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.nav.UnknownRoute

/**
 * Namespace for adaptive layout changes in an app
 */
object Adaptive {

    /**
     * Scope for adaptive content that can show up in an arbitrary [Container]
     */
    interface ContainerScope : AnimatedVisibilityScope {

        val containerState: ContainerState

        val canAnimateSharedElements: Boolean

        @Composable
        fun rememberSharedContent(
            key: Any,
            sharedElement: @Composable (Modifier) -> Unit
        ): @Composable (Modifier) -> Unit
    }

    /**
     * A layout in the hierarchy that hosts an [AppRoute]
     */
    enum class Container {
        Primary, Secondary, TransientPrimary
    }

    /**
     * A spot taken by an [AppRoute] that may be moved in from [Container] to [Container]
     */
    internal enum class Slot {
        One, Two, Three
    }

    /**
     * Information about content in an [Adaptive.Container]
     */
    @Stable
    sealed interface ContainerState {
        val currentRoute: AppRoute?
        val previousRoute: AppRoute?
        val container: Container?
        val adaptation: Adaptation
    }

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
        override val currentRoute: AppRoute?,
        override val previousRoute: AppRoute?,
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

        fun Swap.unaffectedContainers() = Container.entries - setOf(from, to)

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

            val TransientToPrimary = Swap(
                from = Container.TransientPrimary,
                to = Container.Primary
            )

            val TransientDismissal = Swap(
                from = Container.TransientPrimary,
                to = null
            )
        }
    }

    /**
     * Data structure for managing navigation as it adapts to various layout configurations
     */
    @Immutable
    internal data class NavigationState(
        /**
         * Monotonously increasing id for changes in navigation state
         */
        val navId: Int,
        /**
         * The route in the primary navigation container
         */
        val primaryRoute: AppRoute,
        /**
         * The route in the secondary navigation container
         */
        val secondaryRoute: AppRoute?,
        /**
         * The route that will show up in the primary navigation container after back is pressed.
         * This is used to preview the incoming route in the primary navigation container after a
         * back press. If a back destination does not need to be previewed, it will be null.
         */
        val transientPrimaryRoute: AppRoute?,
        /**
         * Describes moves between the primary and secondary navigation containers.
         */
        val adaptation: Adaptation,
        /**
         * A set of route ids that may be returned to.
         */
        val backStackIds: Set<String>,
        /**
         * A set of route ids that are animating out.
         */
        val routeIdsAnimatingOut: Set<String>,
        /**
         * A mapping of route ids to the adaptive slots they are currently in.
         */
        val routeIdsToAdaptiveSlots: Map<String?, Slot>,
        /**
         * A mapping of adaptive container to the routes that were last in them.
         */
        val previousContainersToRoutes: Map<Container, AppRoute?>,
        /**
         * The window size class of the current screen configuration
         */
        val windowSizeClass: WindowSizeClass,
        /**
         * The positionalState of route containers
         */
        val routeContainerPositionalState: RouteContainerPositionalState,
    ) {
        companion object {
            internal val Initial = NavigationState(
                navId = -1,
                primaryRoute = UnknownRoute(Slot.One.name),
                secondaryRoute = null,
                transientPrimaryRoute = null,
                adaptation = Adaptation.Change,
                windowSizeClass = WindowSizeClass.COMPACT,
                routeIdsToAdaptiveSlots = Slot.entries.associateBy(Slot::name),
                backStackIds = emptySet(),
                routeIdsAnimatingOut = emptySet(),
                previousContainersToRoutes = emptyMap(),
                routeContainerPositionalState = UiState().routeContainerState
            )
        }
    }
}

internal fun Adaptive.NavigationState.containerStateFor(
    slot: Adaptive.Slot
): Adaptive.ContainerState {
    val route = routeFor(slot)
    val container = route?.let(::containerFor)
    return Adaptive.SlotContainerState(
        slot = slot,
        currentRoute = route,
        previousRoute = previousContainersToRoutes[container],
        container = container,
        adaptation = adaptation,
    )
}

internal fun Adaptive.NavigationState.slotFor(
    container: Adaptive.Container?
): Adaptive.Slot? = when (container) {
    null -> null
    Adaptive.Container.Primary -> routeIdsToAdaptiveSlots[primaryRoute.id]
    Adaptive.Container.Secondary -> routeIdsToAdaptiveSlots[secondaryRoute?.id]
    Adaptive.Container.TransientPrimary -> routeIdsToAdaptiveSlots[transientPrimaryRoute?.id]
}

internal fun Adaptive.NavigationState.containerFor(
    route: AppRoute
): Adaptive.Container? = when (route.id) {
    primaryRoute.id -> Adaptive.Container.Primary
    secondaryRoute?.id -> Adaptive.Container.Secondary
    transientPrimaryRoute?.id -> Adaptive.Container.TransientPrimary
    else -> null
}

internal fun Adaptive.NavigationState.routeFor(
    slot: Adaptive.Slot
): AppRoute? = when (slot) {
    routeIdsToAdaptiveSlots[primaryRoute.id] -> primaryRoute
    routeIdsToAdaptiveSlots[secondaryRoute?.id] -> secondaryRoute
    routeIdsToAdaptiveSlots[transientPrimaryRoute?.id] -> transientPrimaryRoute
    else -> null
}

internal fun Adaptive.NavigationState.routeFor(
    container: Adaptive.Container
): AppRoute? = when (container) {
    Adaptive.Container.Primary -> primaryRoute
    Adaptive.Container.Secondary -> secondaryRoute
    Adaptive.Container.TransientPrimary -> transientPrimaryRoute
}
