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

import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import com.tunjid.me.scaffold.globalui.*
import com.tunjid.mutator.Mutation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Drives global UI that is common from screen to screen described by a [UiState].
 * This makes it so that these persistent UI elements aren't duplicated, and only animate themselves when they change.
 * This is the default implementation of [GlobalUiController] that other implementations of
 * the same interface should delegate to.
 */

fun FragmentActivity.insetMutations(): Flow<Mutation<UiState>> {
    enableEdgeToEdge()

    val rootView = window.decorView
        .findViewById<ViewGroup>(android.R.id.content)
        .getChildAt(0)

    return callbackFlow {
        rootView.setOnApplyWindowInsetsListener { _, insets ->
            channel.trySend {
                reduceSystemInsets(
                    WindowInsetsCompat.toWindowInsetsCompat(insets),
                    0
                )
                // Consume insets so other views will not see them.
            }
            insets.consumeSystemWindowInsets()
        }
        awaitClose { }
    }
}

private fun UiState.reduceSystemInsets(
    windowInsets: WindowInsetsCompat,
    navBarHeightThreshold: Int
): UiState {
    // Do this once, first call is the size
    val currentSystemUI = systemUI
    val currentStaticSystemUI = currentSystemUI.static

    val statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
    val navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
    val cutouts = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
    val captions = windowInsets.getInsets(WindowInsetsCompat.Type.captionBar())
    val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

    val updatedStaticUI = when {
        currentStaticSystemUI !is DelegateStaticSystemUI -> DelegateStaticSystemUI(
            statusBarSize = statusBars.top,
            navBarSize = navBars.bottom
        )

        navBars.bottom < navBarHeightThreshold -> DelegateStaticSystemUI(
            statusBarSize = statusBars.top,
            navBarSize = navBars.bottom
        )

        else -> currentStaticSystemUI.copy(
            statusBarSize = statusBars.top,
        )
    }

    val updatedDynamicUI = DelegateDynamicSystemUI(
        statusBars = Ingress(top = statusBars.top, bottom = statusBars.bottom),
        navBars = Ingress(top = navBars.top, bottom = navBars.bottom),
        cutouts = Ingress(top = cutouts.top, bottom = cutouts.bottom),
        captions = Ingress(top = captions.top, bottom = captions.bottom),
        ime = Ingress(top = ime.top, bottom = ime.bottom),
        snackbarHeight = currentSystemUI.dynamic.snackbarHeight
    )

    return copy(
        systemUI = DelegateSystemUI(
            static = updatedStaticUI,
            dynamic = updatedDynamicUI
        )
    )
}
