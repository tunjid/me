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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.LocalGlobalUiStateHolder
import com.tunjid.me.scaffold.lifecycle.collectAsStateWithLifecycle
import com.tunjid.me.scaffold.lifecycle.mappedCollectAsStateWithLifecycle
import com.tunjid.me.scaffold.nav.*
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
        val moveableNavFlow = remember {
            navStateHolder.state.moveableNav()
        }
        val moveableNav by moveableNavFlow.collectAsState(MoveableNav())

        val moveKind by remember {
            derivedStateOf { moveableNav.moveKind }
        }
        val containerOneRoute by remember {
            derivedStateOf { moveableNav.containerOneAndRoute.route }
        }
        val containerTwoRoute: AppRoute by remember {
            derivedStateOf { moveableNav.containerTwoAndRoute.route }
        }
        val mainContainer: ContentContainer? by remember {
            derivedStateOf { moveableNav.mainContainer }
        }
        val supportingContainer: ContentContainer? by remember {
            derivedStateOf { moveableNav.supportingContainer }
        }

        val saveableStateHolder: SaveableStateHolder = rememberSaveableStateHolder()

        val containerOneContent = rememberMoveableContainerContent(
            saveableStateHolder = saveableStateHolder,
            route = containerOneRoute
        )
        val containerTwoContent = rememberMoveableContainerContent(
            saveableStateHolder = saveableStateHolder,
            route = containerTwoRoute
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
                    mainContent = {
                        mainContainer.content(
                            containerOneContent = containerOneContent,
                            containerTwoContent = containerTwoContent
                        )
                    },
                    supportingContent = {
                        supportingContainer.content(
                            containerOneContent = containerOneContent,
                            containerTwoContent = containerTwoContent
                        )
                    }
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
private fun ContentContainer?.content(
    containerOneContent: @Composable () -> Unit,
    containerTwoContent: @Composable () -> Unit
) {
    when (this) {
        ContentContainer.One -> containerOneContent()
        ContentContainer.Two -> containerTwoContent()
        null -> Unit
    }
}

@Composable
private fun rememberMoveableContainerContent(
    saveableStateHolder: SaveableStateHolder,
    route: AppRoute
): @Composable () -> Unit {
    return remember(route) {
        movableContentOf {
            saveableStateHolder.SaveableStateProvider(route.id) {
                route.Render()
            }
        }
    }
}

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
