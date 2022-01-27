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

package com.tunjid.me.common.ui.scaffold

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.me.common.AppMutator
import com.tunjid.me.common.component1
import com.tunjid.me.common.component2
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.globalui.navRailVisible
import com.tunjid.me.common.globalui.routeContainerState
import com.tunjid.me.common.nav.NavItem
import com.tunjid.me.common.nav.NavMutator
import com.tunjid.me.common.nav.navItems
import com.tunjid.me.common.nav.navRailRoute
import com.tunjid.me.common.ui.utilities.UiSizes
import com.tunjid.me.common.ui.utilities.countIf
import com.tunjid.me.common.ui.utilities.mappedCollectAsState
import com.tunjid.mutator.accept
import com.tunjid.treenav.switch

@Composable
fun AppNavRail(
    appMutator: AppMutator,
    saveableStateHolder: SaveableStateHolder,
) {
    val (navMutator, globalUiMutator) = appMutator

    val navState by navMutator.state.collectAsState()
    val containerState by globalUiMutator.state.mappedCollectAsState(mapper = UiState::routeContainerState)

    val hasRailRoute by navMutator.state.mappedCollectAsState { it.navRailRoute != null }
    val navRailVisible by globalUiMutator.state.mappedCollectAsState(mapper = UiState::navRailVisible)

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
                navState.navItems.forEach { navItem ->
                    NavRailItem(item = navItem, navMutator = navMutator)
                }
            }
            Box(
                modifier = Modifier.width(navRailContentWidth)
            ) {
                val route = navState.navRailRoute
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
    navMutator: NavMutator,
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
            navMutator.accept { switch(toIndex = item.index) }
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.name
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = item.name, fontSize = 12.sp)
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}