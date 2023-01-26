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

package com.tunjid.me.core.ui.dragdrop

import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView

class ContentView(context: Context) : FrameLayout(context) {

    private val composeView = ComposeView(context).also(::addView)
    private var touchListener: OnTouchListener? = null

    fun setContent(content: @Composable () -> Unit) = composeView.setContent(content)

    fun onAttached(
        dragListener: OnDragListener,
        touchListener: OnTouchListener
    ) {
        composeView.setOnDragListener(dragListener)
        this.touchListener = touchListener
    }

    override fun onInterceptTouchEvent(motionEvent: MotionEvent): Boolean {
        // Spy on events and detect long presses for drag and drop
        touchListener?.onTouch(this, motionEvent)
        return false
    }
}