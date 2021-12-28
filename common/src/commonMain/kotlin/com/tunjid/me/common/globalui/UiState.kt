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

package com.tunjid.me.common.globalui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector

data class ToolbarItem(
    val id: Int,
    val text: String,
    val imageVector: ImageVector? = null,
    val contentDescription: String? = null,
)

data class UiState(
    val toolbarItems: List<ToolbarItem> = listOf(),
    val toolbarShows: Boolean = false,
    val toolbarOverlaps: Boolean = false,
    val toolbarTitle: CharSequence = "",
    val fabIcon: ImageVector = Icons.Default.Done,
    val fabShows: Boolean = false,
    val fabExtended: Boolean = true,
    val fabText: CharSequence = "",
    val backgroundColor: Int = Color.Transparent.toArgb(),
    val snackbarText: CharSequence = "",
    val navBarColor: Int = Color.Transparent.toArgb(),
    val lightStatusBar: Boolean = false,
    val showsBottomNav: Boolean? = null,
    val statusBarColor: Int = Color.Transparent.toArgb(),
    val insetFlags: InsetDescriptor = InsetFlags.ALL,
    val isImmersive: Boolean = false,
    val systemUI: SystemUI = NoOpSystemUI,
    val fabClickListener: (Unit) -> Unit = emptyCallback(),
    val toolbarMenuClickListener: (ToolbarItem) -> Unit = emptyCallback(),
    val altToolbarMenuClickListener: (ToolbarItem) -> Unit = emptyCallback(),
)

private fun <T> emptyCallback(): (T) -> Unit = {}

// Internal state slices for memoizing animations.
// They aggregate the parts of Global UI they react to

internal data class ToolbarState(
    val statusBarSize: Int,
    val visible: Boolean,
    val overlaps: Boolean,
    val toolbarTitle: CharSequence,
    val items: List<ToolbarItem>,
)

internal data class SnackbarPositionalState(
    val bottomNavVisible: Boolean,
    override val ime: Ingress,
    override val navBarSize: Int,
    override val insetDescriptor: InsetDescriptor
) : KeyboardAware

internal data class FabPositionalState(
    val fabVisible: Boolean,
    val bottomNavVisible: Boolean,
    val snackbarHeight: Int,
    override val ime: Ingress,
    override val navBarSize: Int,
    override val insetDescriptor: InsetDescriptor
) : KeyboardAware

internal data class FragmentContainerPositionalState(
    val statusBarSize: Int,
    val toolbarOverlaps: Boolean,
    val bottomNavVisible: Boolean,
    override val ime: Ingress,
    override val navBarSize: Int,
    override val insetDescriptor: InsetDescriptor
) : KeyboardAware

internal data class BottomNavPositionalState(
    val insetDescriptor: InsetDescriptor,
    val bottomNavVisible: Boolean,
    val navBarSize: Int
)


internal val UiState.toolbarState
    get() = ToolbarState(
        items = toolbarItems,
        toolbarTitle = toolbarTitle,
        visible = toolbarShows,
        overlaps = toolbarOverlaps,
        statusBarSize = systemUI.static.statusBarSize,
    )

internal val UiState.fabState
    get() = FabPositionalState(
        fabVisible = fabShows,
        snackbarHeight = systemUI.dynamic.snackbarHeight,
        bottomNavVisible = showsBottomNav == true,
        ime = systemUI.dynamic.ime,
        navBarSize = systemUI.static.navBarSize,
        insetDescriptor = insetFlags
    )

internal val UiState.snackbarPositionalState
    get() = SnackbarPositionalState(
        bottomNavVisible = showsBottomNav == true,
        ime = systemUI.dynamic.ime,
        navBarSize = systemUI.static.navBarSize,
        insetDescriptor = insetFlags
    )

internal val UiState.fabGlyphs
    get() = fabIcon to fabText

internal val UiState.toolbarPosition
    get() = systemUI.static.statusBarSize

internal val UiState.bottomNavPositionalState
    get() = BottomNavPositionalState(
        bottomNavVisible = showsBottomNav == true,
        navBarSize = systemUI.static.navBarSize,
        insetDescriptor = insetFlags
    )

internal val UiState.fragmentContainerState
    get() = FragmentContainerPositionalState(
        statusBarSize = systemUI.static.statusBarSize,
        insetDescriptor = insetFlags,
        toolbarOverlaps = toolbarOverlaps,
        bottomNavVisible = showsBottomNav == true,
        ime = systemUI.dynamic.ime,
        navBarSize = systemUI.static.navBarSize
    )

val UiState.navBarSize get() = systemUI.static.navBarSize

val UiState.statusBarSize get() = systemUI.static.statusBarSize

/**
 * Interface for [UiState] state slices that are aware of the keyboard. Useful for
 * keyboard visibility changes for bottom aligned views like Floating Action Buttons and Snack Bars
 */
interface KeyboardAware {
    val ime: Ingress
    val navBarSize: Int
    val insetDescriptor: InsetDescriptor
}

internal val KeyboardAware.keyboardSize get() = ime.bottom - navBarSize