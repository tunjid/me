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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.max
import com.tunjid.me.common.globalui.GlobalUiMutator
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.globalui.fragmentContainerState
import com.tunjid.me.common.globalui.keyboardSize
import com.tunjid.me.common.ui.countIf
import com.tunjid.me.common.ui.mappedCollectAsState
import com.tunjid.me.common.ui.uiSizes

@Composable
internal fun AppRouteContainer(
    globalUiMutator: GlobalUiMutator,
    content: @Composable BoxScope.() -> Unit
) {
    val state by globalUiMutator.state.mappedCollectAsState(mapper = UiState::fragmentContainerState)

    val topClearanceAnimation = remember { Animatable(0f) }
    val bottomClearanceAnimation = remember { Animatable(0f) }

    val bottomNavHeight = uiSizes.bottomNavSize countIf state.bottomNavVisible
    val insetClearance = max(
        a = bottomNavHeight,
        b = with(LocalDensity.current) { state.keyboardSize.toDp() }
    )
    val navBarClearance = with(LocalDensity.current) {
        state.navBarSize.toDp()
    } countIf state.insetDescriptor.hasBottomInset

    val totalBottomClearance =
        with(LocalDensity.current) { (insetClearance + navBarClearance).toPx() }

    LaunchedEffect(totalBottomClearance) {
        bottomClearanceAnimation.animateTo(totalBottomClearance)
    }

    val statusBarSize = with(LocalDensity.current) {
        state.statusBarSize.toDp()
    } countIf state.insetDescriptor.hasTopInset
    val toolbarHeight = uiSizes.toolbarSize countIf !state.toolbarOverlaps

    val topClearance = with(LocalDensity.current) { (statusBarSize + toolbarHeight).toPx() }
    LaunchedEffect(topClearance) {
        topClearanceAnimation.animateTo(topClearance)
    }

    Box(
        modifier = Modifier.padding(
            top = with(LocalDensity.current) { topClearanceAnimation.value.toDp() },
            bottom = with(LocalDensity.current) { bottomClearanceAnimation.value.toDp() }
        ),
        content = content
    )
}
