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
import android.content.Context
import android.view.DragEvent
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.GestureDetectorCompat
import com.tunjid.me.core.utilities.ContentUri
import com.tunjid.me.core.utilities.RemoteUri
import com.tunjid.me.core.utilities.Uri
import android.net.Uri as AndroidUri

actual class PlatformDragDropModifier(
    context: Context,
) : FrameLayout(context), DragDropModifier by rootDragDropModifier() {

    private val composeView = ComposeView(context)

    init {
        composeView.setOnDragListener(dragListener(this))
        addView(composeView)
    }

    fun setContent(content: @Composable () -> Unit) = composeView.setContent(content)

    private val longPressDragGestureDetector = GestureDetectorCompat(
        context,
        longPressDragGestureListener(
            view = this,
            dragDropModifier = this
        )
    )

    override fun onInterceptTouchEvent(motionEvent: MotionEvent): Boolean {
        // Spy on events and tetect long presses
        longPressDragGestureDetector.onTouchEvent(motionEvent)
        return false
    }
}

private fun longPressDragGestureListener(
    view: View,
    dragDropModifier: DragDropModifier,
) = object : GestureDetector.SimpleOnGestureListener() {
    override fun onLongPress(event: MotionEvent) {
        val dragStatus = dragDropModifier.dragStatus(Offset(event.x, event.y))
        if (dragStatus !is DragStatus.Draggable) return

        val dragData = dragStatus.clipData()

        if (dragData != null) view.startDrag(
            dragData,
            View.DragShadowBuilder(view),
            null,
            0
        )
    }
}

private fun dragListener(
    dragDropModifier: DragDropModifier,
): View.OnDragListener = View.OnDragListener { _, event ->
    when (event.action) {
        DragEvent.ACTION_DRAG_STARTED -> {
            dragDropModifier.onDragStarted(
                uris = listOf(),
                position = Offset(event.x, event.y)
            )
            true
        }

        DragEvent.ACTION_DRAG_ENTERED -> {
            dragDropModifier.onDragEntered()
            true
        }

        DragEvent.ACTION_DRAG_LOCATION -> {
            dragDropModifier.onDragMoved(Offset(event.x, event.y))
            true
        }

        DragEvent.ACTION_DRAG_EXITED -> {
            dragDropModifier.onDragExited()
            true
        }

        DragEvent.ACTION_DROP -> {
            dragDropModifier.onDropped(
                uris = event.uris(),
                position = Offset(event.x, event.y)
            )
        }

        DragEvent.ACTION_DRAG_ENDED -> {
            dragDropModifier.onDragEnded()
            true
        }

        else -> error("Invalid action: ${event.action}")
    }
}

private fun DragStatus.Draggable.clipData(): ClipData? {
    if (uris.isEmpty()) return null

    val mimeTypes = uris.map(Uri::mimetype).distinct().toTypedArray()
    val dragData = ClipData(
        "Drag drop",
        mimeTypes,
        ClipData.Item(AndroidUri.parse(uris.first().path))
    )
    uris.drop(1).forEach { uri ->
        dragData.addItem(ClipData.Item(AndroidUri.parse(uri.path)))
    }
    println("mimeTypes: $mimeTypes; uris: $uris")
    return dragData
}

private fun DragEvent.uris(): List<Uri> = with(clipData) {
    0.until(itemCount).map { itemIndex ->
        with(description) {
            0.until(mimeTypeCount).mapNotNull { mimeTypeIndex ->
                val path = getItemAt(itemIndex)?.uri?.toString() ?: return@mapNotNull null
                val mimeType = getMimeType(mimeTypeIndex)
                when {
                    path.startsWith("http") -> RemoteUri(
                        path = path,
                        mimetype = mimeType
                    )
                }
                ContentUri(
                    path = path,
                    mimetype = getMimeType(mimeTypeIndex)
                )
            }
        }
    }
        .flatten()
}