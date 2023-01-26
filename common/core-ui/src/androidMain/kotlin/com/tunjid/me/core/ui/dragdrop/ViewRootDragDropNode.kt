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
import android.view.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.modifier.ModifierLocalMap
import androidx.compose.ui.modifier.ModifierLocalNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.unit.Density
import androidx.core.view.GestureDetectorCompat
import com.tunjid.me.core.utilities.ContentUri
import com.tunjid.me.core.utilities.RemoteUri
import com.tunjid.me.core.utilities.Uri
import android.net.Uri as AndroidUri

actual class RootDragDropNode : DelegatingNode(),
    ModifierLocalNode,
    GlobalPositionAwareModifierNode,
    View.OnAttachStateChangeListener {

    private val dragDropNode: DragDropNode = delegated { rootDragDropNode() }

    override val providedValues: ModifierLocalMap = dragDropNode.providedValues

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) =
        dragDropNode.onGloballyPositioned(coordinates)

    override fun onViewAttachedToWindow(view: View) {
        check(view is ContentView)
        view.onAttached(
            dragListener = dragListener(
                dragDroppable = dragDropNode
            ),
            touchListener = longPressDragGestureListener(
                view = view,
                dragDroppable = dragDropNode
            )
        )
    }

    override fun onViewDetachedFromWindow(view: View) = Unit
}

private fun longPressDragGestureListener(
    view: View,
    dragDroppable: DragDroppable,
): View.OnTouchListener {
    val gestureDetector = GestureDetectorCompat(
        view.context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(event: MotionEvent) {
                val dragInfo = dragDroppable.dragInfo(Offset(event.x, event.y)) ?: return
                val clipData = dragInfo.clipData() ?: return
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
            }
        }
    )
    return View.OnTouchListener { _, motionEvent -> gestureDetector.onTouchEvent(motionEvent) }
}

private fun dragListener(
    dragDroppable: DragDroppable,
): View.OnDragListener = View.OnDragListener { _, event ->
    when (event.action) {
        DragEvent.ACTION_DRAG_STARTED -> {
            dragDroppable.onStarted(
                mimeTypes = event.startDropMimetypes(),
                position = Offset(event.x, event.y)
            )
            true
        }

        DragEvent.ACTION_DRAG_ENTERED -> {
            dragDroppable.onEntered()
            true
        }

        DragEvent.ACTION_DRAG_LOCATION -> {
            dragDroppable.onMoved(Offset(event.x, event.y))
            true
        }

        DragEvent.ACTION_DRAG_EXITED -> {
            dragDroppable.onExited()
            true
        }

        DragEvent.ACTION_DROP -> {
            dragDroppable.onDropped(
                uris = event.endDropUris(),
                position = Offset(event.x, event.y)
            )
        }

        DragEvent.ACTION_DRAG_ENDED -> {
            dragDroppable.onEnded()
            true
        }

        else -> error("Invalid action: ${event.action}")
    }
}

private fun DragInfo.clipData(): ClipData? {
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

private fun DragEvent.startDropMimetypes() = with(clipDescription) {
    (0 until mimeTypeCount)
        .map(::getMimeType)
        .toSet()
}

private fun DragEvent.endDropUris(): List<Uri> = with(clipData) {
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

                    path.startsWith("content") -> ContentUri(
                        path = path,
                        mimetype = mimeType
                    )

                    else -> null
                }
            }
        }
    }
        .flatten()
}