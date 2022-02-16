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

data class Ingress(
    val top: Int,
    val bottom: Int
)

/**
 * Interface for system managed bits of global ui that we react to, but do not explicitly request
 */
interface SystemUI {
    val static: StaticSystemUI
    val dynamic: DynamicSystemUI
}

/**
 * Static global UI, never changes for a configuration lifecycle. If the app is rotated,
 * a new activity will be created with new configurations.
 */
interface StaticSystemUI {
    val statusBarSize: Int
    val navBarSize: Int
}

interface DynamicSystemUI {
    val statusBars: Ingress
    val navBars: Ingress
    val cutouts: Ingress
    val captions: Ingress
    val ime: Ingress
    val snackbarHeight: Int
}

data class DelegateSystemUI(
    override val static: DelegateStaticSystemUI,
    override val dynamic: DelegateDynamicSystemUI
) : SystemUI

data class DelegateStaticSystemUI(
    override val statusBarSize: Int,
    override val navBarSize: Int
) : StaticSystemUI

data class DelegateDynamicSystemUI internal constructor(
    override val statusBars: Ingress,
    override val navBars: Ingress,
    override val cutouts: Ingress,
    override val captions: Ingress,
    override val ime: Ingress,
    override val snackbarHeight: Int
) : DynamicSystemUI

fun SystemUI.filterNoOp(existing: SystemUI): SystemUI = when {
    // Prioritize existing over no op instances from route changes
    this is NoOpSystemUI && existing !is NoOpSystemUI -> existing
    else -> this
}

fun SystemUI.updateSnackbarHeight(snackbarHeight: Int) = when (this) {
    is DelegateSystemUI -> copy(dynamic = dynamic.copy(snackbarHeight = snackbarHeight))
    else -> this
}

object NoOpSystemUI : SystemUI {
    override val static: StaticSystemUI
        get() = NoOpStaticSystemUI
    override val dynamic: DynamicSystemUI
        get() = NoOpDynamicSystemUI
}

private object NoOpStaticSystemUI : StaticSystemUI {
    override val statusBarSize: Int
        get() = 0
    override val navBarSize: Int
        get() = 0
}

private object NoOpDynamicSystemUI : DynamicSystemUI {
    override val statusBars: Ingress
        get() = emptyInsets
    override val navBars: Ingress
        get() = emptyInsets
    override val cutouts: Ingress
        get() = emptyInsets
    override val captions: Ingress
        get() = emptyInsets
    override val ime: Ingress
        get() = emptyInsets
    override val snackbarHeight: Int
        get() = 0
}

private val emptyInsets = Ingress(0, 0)
