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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.me.core.utilities.countIf
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.navRailWidth
import com.tunjid.me.scaffold.globalui.slices.routeContainerState
import com.tunjid.me.scaffold.globalui.toolbarSize
import com.tunjid.me.scaffold.lifecycle.mappedCollectAsStateWithLifecycle
import com.tunjid.me.scaffold.nav.NavItem
import com.tunjid.me.scaffold.nav.NavStateHolder
import com.tunjid.me.scaffold.nav.navItemSelected
import com.tunjid.me.scaffold.nav.navItems
import com.tunjid.treenav.MultiStackNav

/**
 * Motionally intelligent nav rail shared amongst nav routes in the app
 */
@Composable
internal fun AppNavRail(
    globalUiStateHolder: GlobalUiStateHolder,
    navStateHolder: NavStateHolder,
) {
    val containerState by globalUiStateHolder.state.mappedCollectAsStateWithLifecycle(
        mapper = UiState::routeContainerState
    )
    val windowSizeClass by globalUiStateHolder.state.mappedCollectAsStateWithLifecycle(
        mapper = UiState::windowSizeClass
    )

    val navItems by navStateHolder.state.mappedCollectAsStateWithLifecycle(
        mapper = MultiStackNav::navItems
    )

    val statusBarSize = with(LocalDensity.current) {
        containerState.statusBarSize.toDp()
    } countIf containerState.insetDescriptor.hasTopInset
    val toolbarHeight = windowSizeClass.toolbarSize() countIf !containerState.toolbarOverlaps

    val topClearance by animateDpAsState(targetValue = statusBarSize + toolbarHeight)
    val navRailWidth by animateDpAsState(windowSizeClass.navRailWidth())

    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .width(navRailWidth),
    ) {
        Spacer(modifier = Modifier
            .padding(top = topClearance)
            .height(24.dp)
        )
        navItems.forEach { navItem ->
            NavRailItem(item = navItem, navStateHolder = navStateHolder)
        }
    }
}

@Composable
private fun NavRailItem(
    item: NavItem,
    navStateHolder: NavStateHolder,
) {
    val alpha = if (item.selected) 1f else 0.6f
    NavigationRailItem(
        selected = item.selected,
        icon = {
            Icon(
                imageVector = item.icon,
                contentDescription = item.name,
            )
        },
        label = {
            Text(
                modifier = Modifier.alpha(alpha),
                text = item.name,
                fontSize = 12.sp
            )
        },
        onClick = {
            navStateHolder.accept { navState.navItemSelected(item = item) }
        }
    )
}