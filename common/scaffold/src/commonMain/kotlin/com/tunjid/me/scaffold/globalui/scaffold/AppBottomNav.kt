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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tunjid.me.core.utilities.mappedCollectAsState
import com.tunjid.me.scaffold.globalui.GlobalUiMutator
import com.tunjid.me.scaffold.globalui.UiSizes
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.slices.bottomNavPositionalState
import com.tunjid.me.scaffold.lifecycle.mappedCollectAsStateWithLifecycle
import com.tunjid.me.scaffold.nav.NavMutator
import com.tunjid.me.scaffold.nav.NavState
import com.tunjid.me.scaffold.nav.navItemSelected
import com.tunjid.me.scaffold.nav.navItems

/**
 * Motionally intelligent bottom nav shared amongst nav routes in the app
 */
@Composable
internal fun BoxScope.AppBottomNav(
    globalUiMutator: GlobalUiMutator,
    navMutator: NavMutator,
) {
    val nav by navMutator.state.mappedCollectAsStateWithLifecycle(mapper = NavState::mainNav)
    val state by globalUiMutator.state.mappedCollectAsStateWithLifecycle(mapper = UiState::bottomNavPositionalState)

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
                                navMutator.accept { mainNav.navItemSelected(item = navItem) }
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
