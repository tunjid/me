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

import android.content.ClipData
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.core.view.GestureDetectorCompat
import com.tunjid.me.core.utilities.Uri

sealed class DragTrigger {
    object LongPress : DragTrigger()

    object DoubleTap : DragTrigger()
}

internal class DragTriggerDetector(
    private val view: View,
    internal var dragTriggers: Set<DragTrigger>,
    private val dragDroppable: DragDroppable,
) {

    private val gestureDetector = GestureDetectorCompat(
        view.context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(event: MotionEvent) {
                if (dragTriggers.contains(DragTrigger.LongPress)) processDrag(event)
            }

            override fun onDoubleTap(event: MotionEvent): Boolean =
                if (dragTriggers.contains(DragTrigger.DoubleTap)) processDrag(event)
                else false
        }
    )

    private fun processDrag(event: MotionEvent): Boolean {
        val dragInfo = dragDroppable.dragInfo(Offset(event.x, event.y)) ?: return false
        val clipData = dragInfo.clipData() ?: return false
        val density = with(view.context.resources) {
            Density(
                density = displayMetrics.density,
                fontScale = configuration.fontScale
            )
        }

        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        view.startDrag(
            clipData,
            PainterDragShadowBuilder(
                density = density,
                dragInfo = dragInfo,
            ),
            null,
            0
        )

        return true
    }

    fun onMotionEvent(event: MotionEvent): Boolean =
        gestureDetector.onTouchEvent(event)
}

private fun DragInfo.clipData(): ClipData? {
    if (uris.isEmpty()) return null

    val mimeTypes = uris.map(Uri::mimetype).distinct().toTypedArray()
    val dragData = ClipData(
        "Drag drop",
        mimeTypes,
        ClipData.Item(android.net.Uri.parse(uris.first().path))
    )
    uris.drop(1).forEach { uri ->
        dragData.addItem(ClipData.Item(android.net.Uri.parse(uri.path)))
    }
    println("mimeTypes: $mimeTypes; uris: $uris")
    return dragData
}