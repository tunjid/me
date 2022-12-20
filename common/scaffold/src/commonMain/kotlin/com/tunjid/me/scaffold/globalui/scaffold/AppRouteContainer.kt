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

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.zIndex
import com.tunjid.me.core.utilities.countIf
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.UiSizes
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.keyboardSize
import com.tunjid.me.scaffold.globalui.slices.routeContainerState
import com.tunjid.me.scaffold.lifecycle.mappedCollectAsStateWithLifecycle
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.nav.NavStateHolder
import com.tunjid.me.scaffold.nav.SwapSlot

/**
 * Motionally intelligent container for the hosting the main navigation routes
 */
@Composable
internal fun AppRouteContainer(
    globalUiStateHolder: GlobalUiStateHolder,
    navStateHolder: NavStateHolder,
    navSlot: SwapSlot?,
    mainSlot: SwapSlot?,
    slotOneRoute: AppRoute,
    slotTwoRoute: AppRoute,
    slotOneContent: @Composable () -> Unit,
    slotTwoContent: @Composable () -> Unit,
) {
    println("1: ${slotOneRoute.id}; 2: ${slotTwoRoute.id}; navSLot: $navSlot; mainSlot: $mainSlot")

    val state by globalUiStateHolder.state.mappedCollectAsStateWithLifecycle(mapper = UiState::routeContainerState)

    val bottomNavHeight = UiSizes.bottomNavSize countIf state.bottomNavVisible
    val insetClearance = max(
        a = bottomNavHeight,
        b = with(LocalDensity.current) { state.keyboardSize.toDp() }
    )
    val navBarClearance = with(LocalDensity.current) {
        state.navBarSize.toDp()
    } countIf state.insetDescriptor.hasBottomInset

    val bottomClearance by animateDpAsState(targetValue = insetClearance + navBarClearance)

    val statusBarSize = with(LocalDensity.current) {
        state.statusBarSize.toDp()
    } countIf state.insetDescriptor.hasTopInset
    val toolbarHeight = UiSizes.toolbarSize countIf !state.toolbarOverlaps

    val topClearance by animateDpAsState(targetValue = statusBarSize + toolbarHeight)

    val hasNavContent by navStateHolder.state.mappedCollectAsStateWithLifecycle { it.navRail != null }
    val navRailVisible = state.navRailVisible
    val navRailSize = UiSizes.navRailWidth countIf navRailVisible

    val startClearance by animateDpAsState(targetValue = navRailSize)

    Box(
        modifier = Modifier.padding(
            start = startClearance,
            top = topClearance,
            bottom = bottomClearance
        ),
        content = {
            MoveableNavContent(
                type = SwapSlot.One.type(
                    hasNavContent = hasNavContent,
                    mainSlot = mainSlot,
                    navSlot = navSlot
                ),
                content = slotOneContent
            )
            MoveableNavContent(
                type = SwapSlot.Two.type(
                    hasNavContent = hasNavContent,
                    mainSlot = mainSlot,
                    navSlot = navSlot
                ),
                content = slotTwoContent
            )
        }
    )
}

@Composable
private fun MoveableNavContent(
    type: Type,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier

    ) {
        val width by animateDpAsState(
            targetValue = when (type) {
                Type.Main,
                Type.Detail,
                Type.None -> maxWidth

                Type.Nav -> UiSizes.navRailContentWidth
            },
//            animationSpec = when(type) {
//                Type.Main -> TODO()
//                Type.Nav -> TODO()
//                Type.Detail -> TODO()
//                Type.None -> TODO()
//            }
        )
        val startPadding = if (type == Type.Detail) UiSizes.navRailContentWidth else 0.dp
        val zIndex = remember(type) {
            when (type) {
                Type.Detail,
                Type.Main -> 2f

                Type.Nav -> 1f
                Type.None -> 0f
            }
        }



        Box(
            modifier = Modifier
                .width(width)
                .padding(start = startPadding)
                .zIndex(zIndex)

        ) {
            if (type != Type.None) content()
        }
    }
}

private fun SwapSlot.type(
    hasNavContent: Boolean,
    mainSlot: SwapSlot?,
    navSlot: SwapSlot?,
) = when (this) {
    mainSlot -> if (hasNavContent) Type.Detail else Type.Main
    navSlot -> if (hasNavContent) Type.Nav else Type.None
    else -> Type.None
}

enum class Type {
    Main, Nav, Detail, None
}
