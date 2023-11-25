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

package com.tunjid.me.scaffold.scaffold

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.bottomNavSize
import com.tunjid.me.scaffold.globalui.slices.bottomNavPositionalState
import com.tunjid.me.scaffold.lifecycle.collectAsStateWithLifecycle
import com.tunjid.me.scaffold.lifecycle.mappedCollectAsStateWithLifecycle
import com.tunjid.me.scaffold.nav.NavStateHolder
import com.tunjid.me.scaffold.nav.navItemSelected
import com.tunjid.me.scaffold.nav.navItems

/**
 * Motionally intelligent bottom nav shared amongst nav routes in the app
 */
@Composable
internal fun BoxScope.AppBottomNav(
    globalUiStateHolder: GlobalUiStateHolder,
    navStateHolder: NavStateHolder,
) {
    val nav by navStateHolder.state.collectAsStateWithLifecycle()
    val state by globalUiStateHolder.state.mappedCollectAsStateWithLifecycle(
        mapper = UiState::bottomNavPositionalState
    )
    val windowSizeClass = state.windowSizeClass

    val bottomNavPosition by animateDpAsState(
        when {
            state.bottomNavVisible -> 0.dp
            else -> windowSizeClass.bottomNavSize() + with(LocalDensity.current) { state.navBarSize.toDp() }
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
            windowInsets = WindowInsets(left = 0, top = 0, right = 0, bottom = 0)
        ) {

            nav.navItems
                .forEach { navItem ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = navItem.icon,
                                contentDescription = navItem.name
                            )
                        },
                        label = { Text(navItem.name) },
                        selected = navItem.selected,
                        onClick = {
                            navStateHolder.accept { navState.navItemSelected(item = navItem) }
                        }
                    )
                }
        }
        BottomAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(LocalDensity.current) {
                    state.navBarSize.toDp()
                })
        ){}
    }
}
