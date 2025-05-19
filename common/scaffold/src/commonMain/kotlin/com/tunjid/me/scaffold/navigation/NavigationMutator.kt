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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.data.repository.EmptySavedState
import com.tunjid.me.data.repository.InitialSavedState
import com.tunjid.me.data.repository.SavedState
import com.tunjid.me.data.repository.SavedStateRepository
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.common.scaffold.generated.resources.Res
import me.common.scaffold.generated.resources.articles
import me.common.scaffold.generated.resources.projects
import me.common.scaffold.generated.resources.settings
import me.common.scaffold.generated.resources.talks
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.StringResource

typealias NavigationStateHolder = ActionStateMutator<NavigationMutation, StateFlow<MultiStackNav>>
typealias NavigationMutation = NavigationContext.() -> MultiStackNav

/**
 * An action that causes mutations to navigation
 */
interface NavigationAction {
    val navigationMutation: NavigationMutation
}

data class NavItem(
    val stack: AppStack,
    val index: Int,
    val selected: Boolean
)

@Inject
class PersistedNavigationStateHolder(
    appScope: CoroutineScope,
    savedStateRepository: SavedStateRepository,
    routeParser: RouteParser,
) : NavigationStateHolder by appScope.actionStateFlowMutator(
    initialState = InitialNavigationState,
    started = SharingStarted.Eagerly,
    inputs = listOf(
//        savedStateRepository.forceSignOutMutations()
    ),
    actionTransform = { navActions ->
        flow {
            // Restore saved nav from disk first
            val savedState = savedStateRepository.savedState
                // Wait for a non empty saved state to be read
                .first { it != InitialSavedState }

            val multiStackNav = when {
                savedState == EmptySavedState -> SignedInNavigationState
                else -> routeParser.parseMultiStackNav(savedState)
            }

            emit { multiStackNav }

            emitAll(
                navActions.mapToMutation { navMutation ->
                    navMutation(
                        ImmutableNavigationContext(
                            state = this,
                            routeParser = routeParser
                        )
                    )
                }
            )
        }
    },
    stateTransform = { navigationStateFlow ->
        // Save each new navigation state in parallel
        navigationStateFlow.onEach { navigationState ->
            appScope.persistNavigationState(
                navigationState = navigationState,
                savedStateRepository = savedStateRepository
            )
        }
    }
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

private fun SavedStateRepository.forceSignOutMutations(): Flow<Mutation<MultiStackNav>> =
    savedState
        // No auth token and is displaying main navigation
        .filter { it.auth == null && it != EmptySavedState }
        .mapToMutation { _ ->
            SignedInNavigationState
        }

private fun CoroutineScope.persistNavigationState(
    navigationState: MultiStackNav,
    savedStateRepository: SavedStateRepository,
) = launch {
    if (navigationState != InitialNavigationState) savedStateRepository.updateState {
        this.copy(navigation = navigationState.toSavedState())
    }
}

private fun RouteParser.parseMultiStackNav(savedState: SavedState) =
    savedState.navigation.backStacks
        .foldIndexed(
            initial = MultiStackNav(
                name = SignedInNavigationState.name,
            ),
            operation = { index, multiStackNav, routesForStack ->
                multiStackNav.copy(
                    stacks = multiStackNav.stacks +
                            routesForStack.fold(
                                initial = StackNav(
                                    name = SignedInNavigationState.stacks.getOrNull(index)?.name ?: "Unknown"
                                ),
                                operation = innerFold@{ stackNav, route ->
                                    val resolvedRoute =
                                        parse(pathAndQueries = route) ?: unknownRoute()
                                    stackNav.copy(
                                        children = stackNav.children + resolvedRoute
                                    )
                                }
                            )
                )
            }
        )
        .copy(
            currentIndex = savedState.navigation.activeNav
        )

private fun MultiStackNav.toSavedState() = SavedState.Navigation(
    activeNav = currentIndex,
    backStacks = stacks.fold(listOf()) { listOfLists, stackNav ->
        listOfLists.plus(
            element = stackNav.children
                .filterIsInstance<Route>()
                .fold(listOf()) { stackList, route ->
                    stackList + route.routeParams.pathAndQueries
                }
        )
    },
)

private val InitialNavigationState = MultiStackNav(
    name = "splash-app",
    stacks = listOf(
        StackNav(
            name = AppStack.Articles.stackName,
            children = listOf(routeOf("/archives/${ArchiveKind.Articles.type}"))
        ),
    )
)

private val SignedInNavigationState = MultiStackNav(
    name = "signed-in-app",
    stacks = listOf(
        StackNav(
            name = AppStack.Articles.stackName,
            children = listOf(routeOf("/archives/${ArchiveKind.Articles.type}"))
        ),
        StackNav(
            name = AppStack.Projects.stackName,
            children = listOf(routeOf("/archives/${ArchiveKind.Projects.type}"))
        ),
        StackNav(
            name = AppStack.Talks.stackName,
            children = listOf(routeOf("/archives/${ArchiveKind.Talks.type}"))
        ),
        StackNav(
            name = AppStack.Settings.stackName,
            children = listOf(routeOf("/settings"))
        ),
    )
)

enum class AppStack(
    val stackName: String,
    val titleRes: StringResource,
    val icon: ImageVector,
) {
    Articles(
        stackName = "articles-stack",
        titleRes = Res.string.articles,
        icon = ArchiveKind.Articles.icon,
    ),
    Projects(
        stackName = "projects-stack",
        titleRes = Res.string.projects,
        icon = ArchiveKind.Projects.icon,
    ),
    Talks(
        stackName = "talks-stack",
        titleRes = Res.string.talks,
        icon = ArchiveKind.Talks.icon,
    ),
    Settings(
        stackName = "settings-stack",
        titleRes = Res.string.settings,
        icon = Icons.Rounded.Settings,
    ),
    Auth(
        stackName = "auth-stack",
        titleRes = Res.string.settings,
        icon = Icons.Rounded.Lock,
    ),
}