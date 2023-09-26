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

package com.tunjid.me.scaffold.globalui.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.LocalGlobalUiStateHolder
import com.tunjid.me.scaffold.nav.AdaptiveContainer
import com.tunjid.me.scaffold.nav.AdaptiveNavigationState
import com.tunjid.me.scaffold.nav.NavStateHolder
import com.tunjid.me.scaffold.nav.adaptiveNavigationState
import com.tunjid.me.scaffold.nav.get
import com.tunjid.me.scaffold.nav.primaryContainer
import com.tunjid.me.scaffold.nav.removedRoutes
import com.tunjid.me.scaffold.nav.secondaryContainer
import com.tunjid.me.scaffold.nav.transientPrimaryContainer
import kotlinx.coroutines.flow.map

/**
 * Root scaffold for the app
 */
@Composable
fun Scaffold(
    modifier: Modifier,
    navStateHolder: NavStateHolder,
    globalUiStateHolder: GlobalUiStateHolder,
) {
    CompositionLocalProvider(
        LocalGlobalUiStateHolder provides globalUiStateHolder,
    ) {
        val uiStateFlow = remember {
            globalUiStateHolder.state
        }
        val adaptiveNavigationStateFlow = remember {
            navStateHolder.state.adaptiveNavigationState(uiStateFlow)
        }
        val adaptiveNavigationState by adaptiveNavigationStateFlow.collectAsState(
            AdaptiveNavigationState()
        )

        val moveKind by remember {
            derivedStateOf { adaptiveNavigationState.moveKind }
        }
        val primaryContainer: AdaptiveContainer? by remember {
            derivedStateOf { adaptiveNavigationState.primaryContainer }
        }
        val secondaryContainer: AdaptiveContainer? by remember {
            derivedStateOf { adaptiveNavigationState.secondaryContainer }
        }
        val transientPrimaryContainer: AdaptiveContainer? by remember {
            derivedStateOf { adaptiveNavigationState.transientPrimaryContainer }
        }

        val saveableStateHolder: SaveableStateHolder = rememberSaveableStateHolder()

        val containerContents = rememberAdaptiveContainersToRoutes(
            adaptiveNavigationState = adaptiveNavigationState,
            saveableStateHolder = saveableStateHolder,
        )

        Surface {
            Box(
                modifier = modifier.fillMaxSize()
            ) {
                AppNavRail(
                    globalUiStateHolder = globalUiStateHolder,
                    navStateHolder = navStateHolder,
                )
                AppToolbar(
                    globalUiStateHolder = globalUiStateHolder,
                    navStateHolder = navStateHolder,
                )
                AppRouteContainer(
                    globalUiStateHolder = globalUiStateHolder,
                    navStateHolder = navStateHolder,
                    moveKind = moveKind,
                    primaryContent = {
                        primaryContainer.adaptiveContent(
                            adaptiveContainersToRoutes = containerContents,
                        )
                    },
                    secondaryContent = {
                        secondaryContainer.adaptiveContent(
                            adaptiveContainersToRoutes = containerContents,
                        )
                    },
                    transientPrimaryContent = {
                        transientPrimaryContainer.adaptiveContent(
                            adaptiveContainersToRoutes = containerContents,
                        )
                    },
                )
                AppFab(
                    globalUiStateHolder = globalUiStateHolder,
                )
                AppBottomNav(
                    globalUiStateHolder = globalUiStateHolder,
                    navStateHolder = navStateHolder,
                )
                AppSnackBar(
                    globalUiStateHolder = globalUiStateHolder,
                )
            }
        }

        SavedStateCleanupEffect(
            saveableStateHolder = saveableStateHolder,
            navStateHolder = navStateHolder
        )
    }
}

@Composable
private fun AdaptiveContainer?.adaptiveContent(
    adaptiveContainersToRoutes: SnapshotStateMap<AdaptiveContainer, @Composable () -> Unit>
) {
    adaptiveContainersToRoutes[this]?.invoke() ?: Unit
}

@Composable
private fun rememberAdaptiveContainersToRoutes(
    adaptiveNavigationState: AdaptiveNavigationState,
    saveableStateHolder: SaveableStateHolder,
): SnapshotStateMap<AdaptiveContainer, @Composable () -> Unit> {
    val updatedAdaptiveNavigationState by rememberUpdatedState(adaptiveNavigationState)
    return remember {
        val adaptiveContainersToRoutes = mutableStateMapOf<AdaptiveContainer, @Composable () -> Unit>()
        AdaptiveContainer.entries.forEach { adaptiveContainer ->
            adaptiveContainersToRoutes[adaptiveContainer] = movableContentOf {
                val route by remember {
                    derivedStateOf { updatedAdaptiveNavigationState[adaptiveContainer] }
                }
                saveableStateHolder.SaveableStateProvider(route.id) {
                    route.Render(
                        when (updatedAdaptiveNavigationState.transientPrimaryBackRoute?.id) {
                            route.id -> Modifier.backPreviewModifier()
                            else -> Modifier
                        }
                    )
                }
            }
        }
        adaptiveContainersToRoutes
    }
}

/**
 * Modifier that offers a way to preview content behind the primary content
 */
internal expect fun Modifier.backPreviewModifier(): Modifier

/**
 * Clean up after navigation routes that have been discarded
 */
@Composable
private fun SavedStateCleanupEffect(
    saveableStateHolder: SaveableStateHolder,
    navStateHolder: NavStateHolder,
) {
    val removedRoutesFlow = remember {
        navStateHolder.state
            .map { it.mainNav }
            .removedRoutes()
    }
    LaunchedEffect(Unit) {
        removedRoutesFlow.collect { routes ->
            routes.forEach { route ->
                saveableStateHolder.removeState(route.id)
            }
        }
    }
}
