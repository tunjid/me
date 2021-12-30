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

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.tunjid.me.common.AppMutator
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.globalui.bottomNavPositionalState
import com.tunjid.me.common.nav.navItems
import com.tunjid.me.common.ui.UiSizes
import com.tunjid.me.common.ui.mappedCollectAsState
import com.tunjid.mutator.accept

@Composable
internal fun BoxScope.AppBottomNav(
    appMutator: AppMutator,
) {
    val (navMutator, globalUiMutator) = appMutator
    val nav by navMutator.state.collectAsState()
    val state by globalUiMutator.state.mappedCollectAsState(mapper = UiState::bottomNavPositionalState)

    val bottomNavPositionAnimation = remember { Animatable(0f) }
    val bottomNavPosition = when {
        state.bottomNavVisible -> 0f
        else -> with(LocalDensity.current) { UiSizes.bottomNavSize.toPx() + state.navBarSize }
    }

    LaunchedEffect(bottomNavPosition) {
        bottomNavPositionAnimation.animateTo(bottomNavPosition)
    }

    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .offset(y = with(LocalDensity.current) { bottomNavPositionAnimation.value.toDp() })
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
                                navMutator.accept { copy(currentIndex = navItem.index) }
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
