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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.max
import com.tunjid.me.common.app.AppMutator
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.globalui.keyboardSize
import com.tunjid.me.common.globalui.routeContainerState
import com.tunjid.me.common.nav.navRailRoute
import com.tunjid.me.common.ui.utilities.UiSizes
import com.tunjid.me.common.ui.utilities.countIf
import com.tunjid.me.common.ui.utilities.mappedCollectAsState

/**
 * Motionally intelligent container for the hosting the main navigation routes
 */
@Composable
internal fun AppRouteContainer(
    appMutator: AppMutator,
    content: @Composable BoxScope.() -> Unit
) {
    val navMutator = appMutator.navMutator
    val globalUiMutator = appMutator.globalUiMutator

    val state by globalUiMutator.state.mappedCollectAsState(mapper = UiState::routeContainerState)

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

    val hasNavContent by navMutator.state.mappedCollectAsState { it.navRailRoute != null }
    val navRailVisible = state.navRailVisible
    val navRailSize = UiSizes.navRailWidth countIf navRailVisible
    val navRailContentWidth = UiSizes.navRailContentWidth countIf (hasNavContent && navRailVisible)

    val startClearance by animateDpAsState(targetValue = navRailSize + navRailContentWidth)

    Box(
        modifier = Modifier.padding(
            start = startClearance,
            top = topClearance,
            bottom = bottomClearance
        ),
        content = content
    )
}
