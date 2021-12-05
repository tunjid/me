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

package com.tunjid.me.globalui

import android.graphics.Color
import android.os.Build
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
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
    window.assumeControl()

    val rootView = window.decorView
        .findViewById<ViewGroup>(android.R.id.content)
        .getChildAt(0)

    return callbackFlow {
        rootView.setOnApplyWindowInsetsListener { _, insets ->
            channel.trySend(Mutation {
                reduceSystemInsets(
                    WindowInsetsCompat.toWindowInsetsCompat(insets),
                    0
                )
                // Consume insets so other views will not see them.
            })
            insets.consumeSystemWindowInsets()
        }
        awaitClose { }
    }
}

private fun Window.assumeControl() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val windowAttributes = attributes
        windowAttributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        attributes = windowAttributes
    }
    WindowCompat.setDecorFitsSystemWindows(this, false)
    navigationBarColor = Color.TRANSPARENT
    statusBarColor = Color.TRANSPARENT
}