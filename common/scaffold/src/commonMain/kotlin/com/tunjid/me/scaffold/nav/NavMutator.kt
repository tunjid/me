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

package com.tunjid.me.scaffold.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.me.scaffold.globalui.adaptive.Adaptive
import com.tunjid.me.scaffold.savedstate.SavedState
import com.tunjid.me.scaffold.savedstate.SavedStateRepository
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.mutation
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.Route
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.current
import com.tunjid.treenav.pop
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

const val NavName = "App"

typealias NavStateHolder = ActionStateProducer<NavMutation, StateFlow<NavState>>
typealias NavMutation = NavContext.() -> MultiStackNav

private val RouteTransitionAnimationSpec: FiniteAnimationSpec<Float> = tween(
    durationMillis = 700
)

/**
 * A route that has a id for a [Route] defined in another module
 */
data class ExternalRoute(
    override val id: String,
) : Route

interface AppRoute : Route {
    @Composable
    fun content()

    /**
     * Defines what route to show in the supporting panel alongside this route
     */
    val supportingRoute: String?
        get() = null

    fun transitionsFor(
        state: Adaptive.ContainerState
    ): Adaptive.Transitions = when (state.container) {
        Adaptive.Container.Primary,
        Adaptive.Container.Secondary -> when (state.adaptation) {
            is Adaptive.Adaptation.Change -> Adaptive.Transitions(
                enter = fadeIn(
                    animationSpec = RouteTransitionAnimationSpec,
                    initialAlpha = 0.1f
                ),
                exit = fadeOut(
                    animationSpec =  RouteTransitionAnimationSpec
                )
            )

            is Adaptive.Adaptation.Swap -> Adaptive.Transitions(
                enter = EnterTransition.None,
                exit = ExitTransition.None,
            )
        }

        Adaptive.Container.TransientPrimary,
        null -> Adaptive.Transitions(
            enter = EnterTransition.None,
            exit = ExitTransition.None,
        )
    }
}

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

data class NavState(
    val mainNav: MultiStackNav,
    val secondaryRoute: AppRoute?
)

val EmptyNavState = NavState(
    mainNav = MultiStackNav(
        name = "emptyMultiStack",
        stacks = listOf(
            StackNav(
                name = "emptyStack",
                routes = listOf(UnknownRoute())
            )
        )
    ),
    secondaryRoute = null
)

val NavState.primaryRoute: AppRoute get() = mainNav.current as? AppRoute ?: UnknownRoute()

val NavState.primaryRouteOnBackPress: AppRoute? get() = mainNav.pop().current as? AppRoute

@Inject
class PersistedNavStateHolder(
    appScope: CoroutineScope,
    savedStateRepository: SavedStateRepository,
    routeParser: RouteParser<AppRoute>,
) : NavStateHolder by appScope.actionStateFlowProducer(
    initialState = EmptyNavState,
    started = SharingStarted.Eagerly,
    actionTransform = { navMutations ->
        flow {
            // Restore saved nav from disk first
            val savedState = savedStateRepository.savedState.first { !it.isEmpty }
            val multiStackNav = routeParser.parseMultiStackNav(savedState)

            emit { routeParser.parseNavState(multiStackNav) }
            emitAll(
                navMutations.map { navMutation ->
                    mutation {
                        val newMultiStackNav = navMutation(
                            ImmutableNavContext(
                                state = mainNav,
                                routeParser = routeParser
                            )
                        )
                        routeParser.parseNavState(newMultiStackNav)
                    }
                }
            )
        }
    },
)


fun <Action, State> Flow<Action>.consumeNavActions(
    mutationMapper: (Action) -> NavMutation,
    action: (NavMutation) -> Unit
) = flatMapLatest {
    action(mutationMapper(it))
    emptyFlow<Mutation<State>>()
}

private fun RouteParser<AppRoute>.parseNavState(
    newMultiStackNav: MultiStackNav
) = NavState(
    mainNav = newMultiStackNav,
    secondaryRoute = newMultiStackNav.secondaryRoute?.let(this::parse)
)

fun RouteParser<AppRoute>.parseMultiStackNav(savedState: SavedState) =
    savedState.navigation.fold(
        initial = MultiStackNav(name = "AppNav"),
        operation = { multiStackNav, routesForStack ->
            multiStackNav.copy(
                stacks = multiStackNav.stacks +
                        routesForStack.fold(
                            initial = StackNav(
                                name = routesForStack.firstOrNull() ?: "Unknown"
                            ),
                            operation = innerFold@{ stackNav, route ->
                                stackNav.copy(
                                    routes = stackNav.routes + (
                                            parse(routeString = route) ?: UnknownRoute()
                                            )
                                )
                            }
                        )
            )
        }
    ).copy(currentIndex = savedState.activeNav)