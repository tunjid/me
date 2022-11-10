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
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.me.core.utilities.countIf
import com.tunjid.me.core.utilities.mappedCollectAsState
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.UiSizes
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.navRailVisible
import com.tunjid.me.scaffold.globalui.slices.routeContainerState
import com.tunjid.me.scaffold.lifecycle.mappedCollectAsStateWithLifecycle
import com.tunjid.me.scaffold.nav.*

/**
 * Motionally intelligent nav rail shared amongst nav routes in the app
 */
@Composable
fun AppNavRail(
    globalUiStateHolder: GlobalUiStateHolder,
    navStateHolder: NavStateHolder,
    saveableStateHolder: SaveableStateHolder,
) {
    val navState by navStateHolder.state.collectAsState()
    val containerState by globalUiStateHolder.state.mappedCollectAsStateWithLifecycle(mapper = UiState::routeContainerState)

    val hasRailRoute by navStateHolder.state.mappedCollectAsStateWithLifecycle { it.navRail != null }
    val navRailVisible by globalUiStateHolder.state.mappedCollectAsStateWithLifecycle(mapper = UiState::navRailVisible)

    val statusBarSize = with(LocalDensity.current) {
        containerState.statusBarSize.toDp()
    } countIf containerState.insetDescriptor.hasTopInset
    val toolbarHeight = UiSizes.toolbarSize countIf !containerState.toolbarOverlaps

    val topClearance by animateDpAsState(targetValue = statusBarSize + toolbarHeight)
    val navRailWidth by animateDpAsState(
        targetValue = if (navRailVisible) UiSizes.navRailWidth
        else 0.dp
    )
    val navRailContentWidth by animateDpAsState(
        targetValue = if (navRailVisible && hasRailRoute) UiSizes.navRailContentWidth
        else 0.dp
    )

    Surface(
        modifier = Modifier
            .padding(top = topClearance)
            .fillMaxHeight(),
        color = MaterialTheme.colors.primary
    ) {
        Row {
            Column(
                modifier = Modifier.width(navRailWidth),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                navState.mainNav.navItems.forEach { navItem ->
                    NavRailItem(item = navItem, navStateHolder = navStateHolder)
                }
            }
            Box(
                modifier = Modifier.width(navRailContentWidth)
            ) {
                val route by navStateHolder.state.mappedCollectAsState(mapper = NavState::navRail)
                saveableStateHolder.SaveableStateProvider(key = "nav-rail-${route?.id}") {
                    if (navRailVisible) route?.Render()
                }
            }
        }
    }
}

@Composable
private fun NavRailItem(
    item: NavItem,
    navStateHolder: NavStateHolder,
) {
    Button(
        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 16.dp,
            bottom = 16.dp,
        ),
        onClick = {
            navStateHolder.accept { mainNav.navItemSelected(item = item) }
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val alpha = if (item.selected) 1f else 0.6f
            Icon(
                imageVector = item.icon,
                contentDescription = item.name,
                tint = MaterialTheme.colors.onSurface.copy(
                    alpha = alpha
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                modifier = Modifier.alpha(alpha),
                text = item.name,
                fontSize = 12.sp
            )
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}