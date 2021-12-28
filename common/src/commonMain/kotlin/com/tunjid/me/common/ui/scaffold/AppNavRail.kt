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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.me.common.globalui.GlobalUiMutator
import com.tunjid.me.common.nav.NavItem
import com.tunjid.me.common.nav.NavMutator
import com.tunjid.me.common.nav.navItems
import com.tunjid.me.common.nav.railRoute
import com.tunjid.me.common.ui.UiSizes

@Composable
fun AppNavRail(
    globalUiMutator: GlobalUiMutator,
    navMutator: NavMutator,
) {
    val navState by navMutator.state.collectAsState()

    Row {
        Column(
            modifier = Modifier.width(UiSizes.navRailWidth),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            navState.navItems.forEach { navItem ->
                NavRailItem(navItem)
            }
        }
        Box(
            modifier = Modifier.width(UiSizes.navRailContentWidth)
        ) {
            navState.railRoute?.Render()
        }
    }
}

@Composable
private fun NavRailItem(navItem: NavItem) {
    Icon(
        imageVector = navItem.icon,
        contentDescription = navItem.name
    )
    Text(text = navItem.name)
}