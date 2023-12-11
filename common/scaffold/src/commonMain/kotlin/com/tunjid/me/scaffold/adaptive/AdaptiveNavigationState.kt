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

package com.tunjid.me.scaffold.adaptive

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tunjid.me.scaffold.globalui.BackStatus
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.WindowSizeClass
import com.tunjid.me.scaffold.globalui.slices.RouteContainerPositionalState
import com.tunjid.me.scaffold.globalui.slices.routeContainerState
import com.tunjid.me.scaffold.navigation.AdaptiveRoute
import com.tunjid.me.scaffold.navigation.UnknownRoute

/**
 * Namespace for adaptive layout changes in an app
 */
object Adaptive {

    /**
     * Scope for adaptive content that can show up in an arbitrary [Container]
     */
    @Stable
    interface ContainerScope : AnimatedVisibilityScope {

        val containerState: ContainerState

        val canAnimateSharedElements: Boolean

        fun isCurrentlyShared(key: Any): Boolean

        @Composable
        fun <T> rememberSharedContent(
            key: Any,
            sharedElement: @Composable (T, Modifier) -> Unit
        ): @Composable (T, Modifier) -> Unit
    }

    /**
     * A layout in the hierarchy that hosts an [AdaptiveRoute]
     */
    enum class Container {
        Primary, Secondary, TransientPrimary
    }

    /**
     * A spot taken by an [AdaptiveRoute] that may be moved in from [Container] to [Container]
     */
    internal enum class Slot {
        One, Two, Three
    }

    /**
     * Information about content in an [Adaptive.Container]
     */
    @Stable
    sealed interface ContainerState {
        val currentRoute: AdaptiveRoute?
        val previousRoute: AdaptiveRoute?
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
        override val currentRoute: AdaptiveRoute?,
        override val previousRoute: AdaptiveRoute?,
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
        data class Change(val previewState: BackStatus.PreviewState) : Adaptation()

        /**
         * Routes were swapped in between containers
         */
        data class Swap(
            val from: Container,
            val to: Container?,
        ) : Adaptation()

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
         * Describes moves between the primary and secondary navigation containers.
         */
        val adaptation: Adaptation,
        /**
         * A mapping of [Container] to the routes in them
         */
        val containersToRoutes: Map<Container, AdaptiveRoute?>,
        /**
         * A mapping of route ids to the adaptive slots they are currently in.
         */
        val routeIdsToAdaptiveSlots: Map<String?, Slot>,
        /**
         * A mapping of adaptive container to the routes that were last in them.
         */
        val previousContainersToRoutes: Map<Container, AdaptiveRoute?>,
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
        val windowSizeClass: WindowSizeClass,
        /**
         * The positionalState of route containers
         */
        val routeContainerPositionalState: RouteContainerPositionalState,
    ) {
        companion object {
            internal val Initial = NavigationState(
                navId = -1,
                adaptation = Adaptation.Change(previewState = BackStatus.PreviewState.NoPreview),
                windowSizeClass = WindowSizeClass.COMPACT,
                containersToRoutes = mapOf(Container.Primary to UnknownRoute(Slot.One.name)),
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
    else -> routeIdsToAdaptiveSlots[containersToRoutes[container]?.id]
}

internal fun Adaptive.NavigationState.containerFor(
    route: AdaptiveRoute
): Adaptive.Container? = containersToRoutes.firstNotNullOfOrNull { (container, containerRoute) ->
    if (containerRoute?.id == route.id) container else null
}

internal fun Adaptive.NavigationState.routeFor(
    slot: Adaptive.Slot
): AdaptiveRoute? = routeIdsToAdaptiveSlots.firstNotNullOfOrNull { (routeId, routeSlot) ->
    if (routeSlot == slot) containersToRoutes.firstNotNullOfOrNull { (_, route) ->
        if (route?.id == routeId) route
        else null
    }
    else null
}

internal fun Adaptive.NavigationState.routeFor(
    container: Adaptive.Container
): AdaptiveRoute? = containersToRoutes[container]
