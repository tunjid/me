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
    val id: String,
    val text: String,
    val imageVector: ImageVector? = null,
    val contentDescription: String? = null,
)

sealed class NavMode {
    object BottomNav : NavMode()
    object NavRail : NavMode()
}

sealed class NavVisibility {
    object Visible : NavVisibility()
    object Gone : NavVisibility()
    object GoneIfBottomNav : NavVisibility()
}

data class UiState(
    val toolbarItems: List<ToolbarItem> = listOf(),
    val toolbarShows: Boolean = false,
    val toolbarOverlaps: Boolean = false,
    val toolbarTitle: CharSequence = "",
    val fabIcon: ImageVector = Icons.Default.Done,
    val fabShows: Boolean = false,
    val fabExtended: Boolean = true,
    val fabText: String = "",
    val backgroundColor: Int = Color.Transparent.toArgb(),
    val snackbarText: CharSequence = "",
    val navBarColor: Int = Color.Transparent.toArgb(),
    val lightStatusBar: Boolean = false,
    val navMode: NavMode = NavMode.BottomNav,
    val navVisibility: NavVisibility = NavVisibility.Visible,
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
    val navRailVisible: Boolean,
    val toolbarTitle: CharSequence,
    val items: List<ToolbarItem>,
)

internal data class SnackbarPositionalState(
    val bottomNavVisible: Boolean,
    override val ime: Ingress,
    override val navBarSize: Int,
    override val insetDescriptor: InsetDescriptor
) : KeyboardAware

internal data class FabState(
    val fabVisible: Boolean,
    val bottomNavVisible: Boolean,
    val snackbarHeight: Int,
    val icon: ImageVector,
    val extended: Boolean,
    val text: String,
    override val ime: Ingress,
    override val navBarSize: Int,
    override val insetDescriptor: InsetDescriptor
) : KeyboardAware

internal data class RouteContainerPositionalState(
    val statusBarSize: Int,
    val toolbarOverlaps: Boolean,
    val navRailVisible: Boolean,
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
        navRailVisible = navRailVisible,
        statusBarSize = statusBarSize,
    )

internal val UiState.fabState
    get() = FabState(
        fabVisible = fabShows,
        snackbarHeight = systemUI.dynamic.snackbarHeight,
        bottomNavVisible = bottomNavVisible,
        icon = fabIcon,
        text = fabText,
        extended = fabExtended,
        ime = systemUI.dynamic.ime,
        navBarSize = systemUI.static.navBarSize,
        insetDescriptor = insetFlags
    )

internal val UiState.snackbarPositionalState
    get() = SnackbarPositionalState(
        bottomNavVisible = bottomNavVisible,
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
        bottomNavVisible = bottomNavVisible,
        navBarSize = systemUI.static.navBarSize,
        insetDescriptor = insetFlags
    )

internal val UiState.routeContainerState
    get() = RouteContainerPositionalState(
        statusBarSize = systemUI.static.statusBarSize,
        insetDescriptor = insetFlags,
        toolbarOverlaps = toolbarOverlaps,
        bottomNavVisible = bottomNavVisible,
        navRailVisible = navRailVisible,
        ime = systemUI.dynamic.ime,
        navBarSize = systemUI.static.navBarSize
    )

val UiState.navBarSize get() = systemUI.static.navBarSize

val UiState.statusBarSize get() = systemUI.static.statusBarSize

val UiState.bottomNavVisible get() = navMode is NavMode.BottomNav && when(navVisibility) {
    NavVisibility.Visible -> true
    NavVisibility.Gone,
    NavVisibility.GoneIfBottomNav -> false
}

val UiState.navRailVisible get() = navMode is NavMode.NavRail && when(navVisibility) {
    NavVisibility.Visible,
    NavVisibility.GoneIfBottomNav -> true
    NavVisibility.Gone -> false
}

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