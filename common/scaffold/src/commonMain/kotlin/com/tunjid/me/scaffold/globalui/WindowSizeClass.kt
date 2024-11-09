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
import androidx.window.core.layout.WindowSizeClass

val WindowSizeClass.Companion.COMPACT get() = WINDOW_SIZE_CLASS_COMPACT

val WindowSizeClass.Companion.MEDIUM get() = WINDOW_SIZE_CLASS_MEDIUM

val WindowSizeClass.Companion.EXPANDED get() = WINDOW_SIZE_CLASS_EXPANDED

fun WindowSizeClass.navRailWidth() =
    when (minWidthDp) {
        in WindowSizeClass.MEDIUM.minWidthDp..Int.MAX_VALUE -> 72.dp
        else -> 0.dp
    }

@Suppress("UnusedReceiverParameter")
fun WindowSizeClass.bottomNavSize() = 80.dp

private val WINDOW_SIZE_CLASS_COMPACT = WindowSizeClass(
    minWidthDp = 0,
    minHeightDp = 0,
)

private val WINDOW_SIZE_CLASS_MEDIUM = WindowSizeClass(
    minWidthDp = WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
    minHeightDp = WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND,
)

private val WINDOW_SIZE_CLASS_EXPANDED = WindowSizeClass(
    minWidthDp = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND,
    minHeightDp = WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND,
)