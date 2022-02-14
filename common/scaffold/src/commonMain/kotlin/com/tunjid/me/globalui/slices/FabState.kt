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

package com.tunjid.me.globalui.slices

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.tunjid.me.globalui.Ingress
import com.tunjid.me.globalui.InsetDescriptor
import com.tunjid.me.globalui.KeyboardAware
import com.tunjid.me.globalui.UiState
import com.tunjid.me.globalui.bottomNavVisible

internal data class FabState(
    val fabVisible: Boolean,
    val bottomNavVisible: Boolean,
    val snackbarOffset: Dp,
    val icon: ImageVector,
    val extended: Boolean,
    val enabled: Boolean,
    val text: String,
    override val ime: Ingress,
    override val navBarSize: Int,
    override val insetDescriptor: InsetDescriptor
) : KeyboardAware

internal val UiState.fabState
    get() = FabState(
        fabVisible = fabShows,
        snackbarOffset = snackbarOffset,
        bottomNavVisible = bottomNavVisible,
        icon = fabIcon,
        text = fabText,
        extended = fabExtended,
        enabled = fabEnabled,
        ime = systemUI.dynamic.ime,
        navBarSize = systemUI.static.navBarSize,
        insetDescriptor = insetFlags
    )