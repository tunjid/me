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

package com.tunjid.me.globalui.scaffold

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.BottomAppBar
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tunjid.me.core.utilities.mappedCollectAsState
import com.tunjid.me.globalui.GlobalUiMutator
import com.tunjid.me.globalui.UiSizes
import com.tunjid.me.globalui.UiState
import com.tunjid.me.globalui.slices.bottomNavPositionalState
import com.tunjid.me.nav.NavMutator
import com.tunjid.me.nav.navItemSelected
import com.tunjid.me.nav.navItems
import com.tunjid.mutator.accept

/**
 * Motionally intelligent bottom nav shared amongst nav routes in the app
 */
@Composable
internal fun BoxScope.AppBottomNav(
    globalUiMutator: GlobalUiMutator,
    navMutator: NavMutator,
) {
    val nav by navMutator.state.collectAsState()
    val state by globalUiMutator.state.mappedCollectAsState(mapper = UiState::bottomNavPositionalState)

    val bottomNavPosition by animateDpAsState(
        when {
            state.bottomNavVisible -> 0.dp
            else -> UiSizes.bottomNavSize + with(LocalDensity.current) { state.navBarSize.toDp() }
        }
    )

    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .offset(y = bottomNavPosition)
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        BottomAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            backgroundColor = MaterialTheme.colors.primary,
        ) {

            BottomNavigation(
                backgroundColor = MaterialTheme.colors.primary,
            ) {
                nav.navItems
                    .forEach { navItem ->
                        BottomNavigationItem(
                            icon = {
                                Icon(
                                    imageVector = navItem.icon,
                                    contentDescription = navItem.name
                                )
                            },
                            label = { Text(navItem.name) },
                            selected = navItem.selected,
                            onClick = {
                                navMutator.accept { navItemSelected(item = navItem) }
                            }
                        )
                    }
            }
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(LocalDensity.current) {
                    state.navBarSize.toDp()
                })
                .background(color = MaterialTheme.colors.primary)
        )
    }
}
