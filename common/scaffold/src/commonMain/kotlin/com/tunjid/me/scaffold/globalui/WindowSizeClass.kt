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

package com.tunjid.me.scaffold.globalui

import androidx.compose.ui.unit.dp

enum class WindowSizeClass { COMPACT, MEDIUM, EXPANDED }

val WindowSizeClass.isNotExpanded get() = this != WindowSizeClass.EXPANDED

fun WindowSizeClass.navRailWidth() =
    when (this) {
        WindowSizeClass.COMPACT -> 0.dp
        WindowSizeClass.MEDIUM,
        WindowSizeClass.EXPANDED -> 72.dp
    }


fun WindowSizeClass.toolbarSize() =
    when (this) {
        WindowSizeClass.COMPACT -> 56.dp
        WindowSizeClass.MEDIUM,
        WindowSizeClass.EXPANDED -> 72.dp
    }

fun WindowSizeClass.bottomNavSize() =
    when (this) {
        WindowSizeClass.COMPACT,
        WindowSizeClass.MEDIUM,
        WindowSizeClass.EXPANDED -> 80.dp
    }

fun WindowSizeClass.supportingPanelWidth() =
    when (this) {
        WindowSizeClass.COMPACT,
        WindowSizeClass.MEDIUM  -> 0.dp
        WindowSizeClass.EXPANDED -> 400.dp
    }