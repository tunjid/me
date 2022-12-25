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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.LocalGlobalUiStateHolder
import com.tunjid.me.scaffold.nav.*

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
            navStateHolder.moveableNav()
        }
        val moveableNav by moveableNavFlow.collectAsState(MoveableNav())

        val moveKind by remember {
            derivedStateOf { moveableNav.moveKind }
        }
        val slotOneRoute by remember {
            derivedStateOf { moveableNav.slotOneAndRoute.route }
        }
        val slotTwoRoute: AppRoute by remember {
            derivedStateOf { moveableNav.slotTwoAndRoute.route }
        }
        val mainSlot: SwapSlot? by remember {
            derivedStateOf { moveableNav.mainSlot }
        }
        val navRailSlot: SwapSlot? by remember {
            derivedStateOf { moveableNav.navSlot }
        }

        val saveableStateHolder: SaveableStateHolder = rememberSaveableStateHolder()

        val slotOneContent = rememberMoveableNav(
            saveableStateHolder = saveableStateHolder,
            route = slotOneRoute
        )
        val slotTwoContent = rememberMoveableNav(
            saveableStateHolder = saveableStateHolder,
            route = slotTwoRoute
        )

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
                    mainSlot.content(
                        slotOneContent = slotOneContent,
                        slotTwoContent = slotTwoContent
                    )
                },
                navRailContent = {
                    navRailSlot.content(
                        slotOneContent = slotOneContent,
                        slotTwoContent = slotTwoContent
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
}

@Composable
private fun SwapSlot?.content(
    slotOneContent: @Composable () -> Unit,
    slotTwoContent: @Composable () -> Unit
) {
    when (this) {
        SwapSlot.One -> slotOneContent()
        SwapSlot.Two -> slotTwoContent()
        null -> Unit
    }
}

@Composable
private fun rememberMoveableNav(
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
