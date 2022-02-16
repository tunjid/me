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

package com.tunjid.me.scaffold.globalui.slices

import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.navRailVisible
import com.tunjid.me.scaffold.globalui.statusBarSize

internal data class ToolbarState(
    val statusBarSize: Int,
    val visible: Boolean,
    val overlaps: Boolean,
    val navRailVisible: Boolean,
    val toolbarTitle: CharSequence,
    val items: List<ToolbarItem>,
)

internal val UiState.toolbarState
    get() = ToolbarState(
        items = toolbarItems,
        toolbarTitle = toolbarTitle,
        visible = toolbarShows,
        overlaps = toolbarOverlaps,
        navRailVisible = navRailVisible,
        statusBarSize = statusBarSize,
    )

data class ToolbarItem(
    val id: String,
    val text: String,
    val imageVector: ImageVector? = null,
    val contentDescription: String? = null,
)
