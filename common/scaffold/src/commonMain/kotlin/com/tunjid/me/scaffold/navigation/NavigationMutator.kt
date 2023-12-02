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

package com.tunjid.me.scaffold.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.me.scaffold.adaptive.Adaptive
import com.tunjid.me.scaffold.savedstate.SavedState
import com.tunjid.me.scaffold.savedstate.SavedStateRepository
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.mutation
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

typealias NavigationStateHolder = ActionStateProducer<NavigationMutation, StateFlow<MultiStackNav>>
typealias NavigationMutation = NavigationContext.() -> MultiStackNav

/**
 * An action that causes mutations to navigation
 */
interface NavigationAction {
    val navigationMutation: NavigationMutation
}

private val RouteTransitionAnimationSpec: FiniteAnimationSpec<Float> = tween(
    durationMillis = 700
)

/**
 * A route that has a id for a [Route] defined in another module
 */
data class ExternalRoute(
    val path: String,
) : AppRoute, StatelessRoute {

    override val routeParams: RouteParams = RouteParams(
        route = path,
        pathArgs = emptyMap(),
        queryParams = emptyMap(),
    )

    // Does not render, just used during traversal to find secondary routes associated with a route
    @Composable
    override fun content() = Unit
}

interface AppRoute : Route {

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

/**
 * [AppRoute] instances with no state holder
 */
interface StatelessRoute : AppRoute

data class NavItem(
    val name: String,
    val icon: ImageVector,
    val index: Int,
    val selected: Boolean
)

private val EmptyNavigationState = MultiStackNav(
    name = "emptyMultiStack",
    stacks = listOf(
        StackNav(
            name = "emptyStack",
            children = listOf(UnknownRoute())
        )
    )
)

@Inject
class PersistedNavigationStateHolder(
    appScope: CoroutineScope,
    savedStateRepository: SavedStateRepository,
    routeParser: RouteParser<AppRoute>,
) : NavigationStateHolder by appScope.actionStateFlowProducer(
    initialState = EmptyNavigationState,
    started = SharingStarted.Eagerly,
    actionTransform = { navMutations ->
        flow {
            // Restore saved nav from disk first
            val savedState = savedStateRepository.savedState.first { !it.isEmpty }
            val multiStackNav = routeParser.parseMultiStackNav(savedState)

            emit { multiStackNav }
            emitAll(
                navMutations.map { navMutation ->
                    mutation {
                        navMutation(
                            ImmutableNavigationContext(
                                state = this,
                                routeParser = routeParser
                            )
                        )
                    }
                }
            )
        }
    },
)

/**
 * A helper function for generic state producers to consume navigation actions
 */
fun <Action : NavigationAction, State> Flow<Action>.consumeNavigationActions(
    navigationMutationConsumer: (NavigationMutation) -> Unit
) = flatMapLatest { action ->
    navigationMutationConsumer(action.navigationMutation)
    emptyFlow<Mutation<State>>()
}

private fun RouteParser<AppRoute>.parseMultiStackNav(savedState: SavedState) =
    savedState.navigation
        .fold(
            initial = MultiStackNav(name = "AppNav"),
            operation = { multiStackNav, routesForStack ->
                multiStackNav.copy(
                    stacks = multiStackNav.stacks +
                            routesForStack.fold(
                                initial = StackNav(
                                    name = routesForStack.firstOrNull() ?: "Unknown"
                                ),
                                operation = innerFold@{ stackNav, route ->
                                    val resolvedRoute = parse(routeString = route) ?: UnknownRoute()
                                    stackNav.copy(
                                        children = stackNav.children + resolvedRoute
                                    )
                                }
                            )
                )
            }
        )
        .copy(
            currentIndex = savedState.activeNav
        )