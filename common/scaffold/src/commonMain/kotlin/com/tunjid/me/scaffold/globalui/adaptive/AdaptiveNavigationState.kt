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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.WindowSizeClass
import com.tunjid.me.scaffold.globalui.adaptive.Adaptive.Adaptation.Change.unaffectedContainers
import com.tunjid.me.scaffold.globalui.isPreviewing
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.nav.NavState
import com.tunjid.me.scaffold.nav.UnknownRoute
import com.tunjid.me.scaffold.nav.primaryRoute
import com.tunjid.me.scaffold.nav.primaryRouteOnBackPress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.scan

/**
 * Namespace for adaptive layout changes in an app
 */
object Adaptive {

    /**
     * Scope for adaptive content that can show up in an arbitrary [Container]
     */
    interface ContainerScope : AnimatedVisibilityScope {

        val animatedModifier: Modifier

        val adaptation: Adaptation

        @Composable
        fun rememberSharedContent(
            key: Any,
            sharedElement: @Composable (Modifier) -> Unit
        ): @Composable (Modifier) -> Unit

        @Composable
        fun isInPreview(): Boolean
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

    @Stable
    // TODO: Why can't the state backed by delegates be seen?
    @Suppress("CanSealedSubClassBeObject")
    internal class MutableContainerState : ContainerState {
        override var currentRoute: AppRoute? by mutableStateOf(null)
        override var previousRoute: AppRoute? by mutableStateOf(null)
        override var container: Container? by mutableStateOf(null)
        override var adaptation: Adaptation by mutableStateOf(Adaptation.Change)
    }

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
            val to: Container,
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
        }
    }

    /**
     * Data structure for managing navigation as it adapts to various layout configurations
     */
    @Immutable
    internal data class NavigationState(
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
    ) {
        companion object {
            internal val Initial = NavigationState(
                primaryRoute = UnknownRoute(Slot.One.name),
                secondaryRoute = null,
                transientPrimaryRoute = null,
                adaptation = Adaptation.Change,
                windowSizeClass = WindowSizeClass.COMPACT,
                routeIdsToAdaptiveSlots = Slot.entries.associateBy(Slot::name),
                previousContainersToRoutes = emptyMap(),
            )
        }
    }
}

private val AdaptiveRouteInContainerLookups = listOf(
    Adaptive.NavigationState::primaryRoute,
    Adaptive.NavigationState::secondaryRoute,
    Adaptive.NavigationState::transientPrimaryRoute,
)

@Composable
internal fun Adaptive.NavigationState.containerStateFor(
    slot: Adaptive.Slot
): Adaptive.ContainerState {
    val containerState = remember(slot) { Adaptive.MutableContainerState() }
    Snapshot.withMutableSnapshot {
        containerState.currentRoute = routeFor(slot)
        containerState.container = containerState.currentRoute?.let(::containerFor)
        containerState.adaptation = adaptation
    }
    return containerState
}

internal fun Adaptive.NavigationState.slotFor(
    container: Adaptive.Container
): Adaptive.Slot? = when (container) {
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

internal fun StateFlow<NavState>.adaptiveNavigationState(
    uiStateFlow: StateFlow<UiState>
): Flow<Adaptive.NavigationState> = combine(uiStateFlow) { navState, uiState ->
    // If there is a back preview in progress, show the back primary route in the
    // primary container
    val visiblePrimaryRoute = navState.primaryRouteOnBackPress.takeIf {
        uiState.backStatus.isPreviewing
    } ?: navState.primaryRoute

    Adaptive.NavigationState(
        primaryRoute = visiblePrimaryRoute,
        secondaryRoute = navState.secondaryRoute.takeIf { route ->
            route?.id != visiblePrimaryRoute.id
                    && uiState.windowSizeClass > WindowSizeClass.COMPACT
        },
        transientPrimaryRoute = navState.primaryRoute.takeIf { route ->
            uiState.backStatus.isPreviewing
                    && route.id != visiblePrimaryRoute.id
                    && route.id != navState.secondaryRoute?.id
        },
        windowSizeClass = uiState.windowSizeClass,
        adaptation = when {
            uiState.backStatus.isPreviewing -> Adaptive.Adaptation.Swap(
                from = Adaptive.Container.Primary,
                to = Adaptive.Container.TransientPrimary,
            )

            else -> Adaptive.Adaptation.Change
        },
        routeIdsToAdaptiveSlots = emptyMap(),
        previousContainersToRoutes = emptyMap(),
    )
}
    .distinctUntilChanged()
    .scan(
        initial = Adaptive.NavigationState.Initial,
        operation = Adaptive.NavigationState::adaptTo
    )

/**
 * A method that adapts changes in navigation to different containers while allowing for them
 * to be animated easily.
 */
private fun Adaptive.NavigationState.adaptTo(
    current: Adaptive.NavigationState,
): Adaptive.NavigationState {
    val adaptation = when (current.adaptation) {
        Adaptive.Adaptation.PrimaryToTransient -> current.adaptation
        else -> when {
            primaryRoute.id == current.secondaryRoute?.id -> Adaptive.Adaptation.PrimaryToSecondary
            current.primaryRoute.id == secondaryRoute?.id -> Adaptive.Adaptation.SecondaryToPrimary
            adaptation == Adaptive.Adaptation.PrimaryToTransient
                    && current.transientPrimaryRoute == null -> Adaptive.Adaptation.TransientToPrimary

            else -> Adaptive.Adaptation.Change
        }
    }

    return when (adaptation) {
        // In a change, each container should keep its slot from the previous state.
        // This allows the AnimatedContent transition run on the route id
        Adaptive.Adaptation.Change -> {
            val updatedRouteIdsToAdaptiveSlots = mutableMapOf<String, Adaptive.Slot>()
            // For routes in all containers, look up its previous slot
            // If that slot is null, find the first slot that hasn't been taken up yet
            // otherwise reuse its existing slot
            for (lookup in AdaptiveRouteInContainerLookups) {
                val currentRoute = lookup(current) ?: continue
                val previousRoute = lookup(this)
                val slot = when (val previousSlot = routeIdsToAdaptiveSlots[previousRoute?.id]) {
                    null -> routeIdsToAdaptiveSlots.entries.first { entry ->
                        !updatedRouteIdsToAdaptiveSlots.containsValue(entry.value)
                    }.value

                    else -> previousSlot
                }
                updatedRouteIdsToAdaptiveSlots[currentRoute.id] = slot
            }
            // TODO: Remove stale route ids after they complete their transition
            current.copy(
                adaptation = adaptation,
                previousContainersToRoutes = Adaptive.Container.entries.associateWith(::routeFor),
                routeIdsToAdaptiveSlots = routeIdsToAdaptiveSlots + updatedRouteIdsToAdaptiveSlots
            )
        }

        // In a swap, preserve the existing slot for a route, however find new routes coming in
        // an assign unoccupied slots to them.
        is Adaptive.Adaptation.Swap -> {
            val fromSlot = this.slotFor(adaptation.from)
            val excludedSlots = adaptation.unaffectedContainers()
                .map(::slotFor)
                .plus(fromSlot)
                .toSet()

            val vacatedSlot = Adaptive.Slot.entries.first {
                !excludedSlots.contains(it)
            }
            current.copy(
                adaptation = adaptation,
                previousContainersToRoutes = Adaptive.Container.entries.associateWith(::routeFor),
                routeIdsToAdaptiveSlots = when (val newRoute = current.routeFor(adaptation.from)) {
                    null -> routeIdsToAdaptiveSlots
                    else -> routeIdsToAdaptiveSlots - routeFor(vacatedSlot)?.id + Pair(
                        first = newRoute.id,
                        second = vacatedSlot
                    )
                }
            )
        }
    }
}
