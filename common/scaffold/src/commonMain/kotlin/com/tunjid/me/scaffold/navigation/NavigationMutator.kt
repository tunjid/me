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

// See YouTrack: KTIJ-18375
@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")
package com.tunjid.me.scaffold.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.me.scaffold.adaptive.AdaptiveRoute
import com.tunjid.me.scaffold.savedstate.SavedState
import com.tunjid.me.scaffold.savedstate.SavedStateRepository
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.mutationOf 
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
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

typealias NavigationStateHolder = ActionStateMutator<NavigationMutation, StateFlow<MultiStackNav>>
typealias NavigationMutation = NavigationContext.() -> MultiStackNav

/**
 * An action that causes mutations to navigation
 */
interface NavigationAction {
    val navigationMutation: NavigationMutation
}

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
    routeParser: RouteParser<AdaptiveRoute>,
) : NavigationStateHolder by appScope.actionStateFlowMutator(
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
                    mutationOf {
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

private fun RouteParser<AdaptiveRoute>.parseMultiStackNav(savedState: SavedState) =
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