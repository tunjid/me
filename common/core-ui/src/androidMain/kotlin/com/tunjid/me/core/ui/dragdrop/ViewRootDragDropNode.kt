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
import android.view.DragEvent
import android.view.View
import androidx.compose.foundation.draganddrop.DragAndDropSourceScope
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.tunjid.me.core.utilities.ContentUri
import com.tunjid.me.core.utilities.RemoteUri
import com.tunjid.me.core.utilities.Uri

actual fun Modifier.dragSource(
    drawDragDecoration: (DrawScope.() -> Unit)?,
    uris: List<Uri>,
): Modifier {
    return when (drawDragDecoration) {
        null -> dragAndDropSource(
            block = {
                triggerDrag(uris)
            }
        )

        else -> dragAndDropSource(
            drawDragDecoration = drawDragDecoration,
            block = {
                triggerDrag(uris)
            }
        )
    }
}

@Composable
actual fun Modifier.dropTarget(
    onStarted: (mimeTypes: Set<String>, Offset) -> Boolean,
    onEntered: () -> Unit,
    onMoved: (position: Offset) -> Unit,
    onExited: () -> Unit,
    onDropped: (uris: List<Uri>, position: Offset) -> Boolean,
    onEnded: () -> Unit,
): Modifier {
    val updatedOnStart by rememberUpdatedState(onStarted)
    val updatedOnEntered by rememberUpdatedState(onEntered)
    val updatedOnMove by rememberUpdatedState(onMoved)
    val updatedOnExit by rememberUpdatedState(onExited)
    val updatedOnDrop by rememberUpdatedState(onDropped)
    val updatedOnEnded by rememberUpdatedState(onEnded)

    return dragAndDropTarget(
        shouldStartDragAndDrop = { startEvent ->
            val androidDragEvent = startEvent.toAndroidDragEvent()
            updatedOnStart(
                startEvent.mimeTypes(),
                Offset(androidDragEvent.x, androidDragEvent.y)
            ).also {
                println("Response: $it; SAW: ${startEvent.mimeTypes()}")
            }
        },
        target = remember {
            object : DragAndDropTarget {
                override fun onEntered(event: DragAndDropEvent) {
                    updatedOnEntered()
                }

                override fun onMoved(event: DragAndDropEvent) {
                    val androidDragEvent = event.toAndroidDragEvent()
                    updatedOnMove(Offset(androidDragEvent.x, androidDragEvent.y))
                }

                override fun onExited(event: DragAndDropEvent) {
                    updatedOnExit()
                }

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    val androidDragEvent = event.toAndroidDragEvent()
                    return updatedOnDrop(
                        androidDragEvent.endDropUris(),
                        Offset(androidDragEvent.x, androidDragEvent.y)
                    )
                }

                override fun onEnded(event: DragAndDropEvent) {
                    updatedOnEnded()
                }
            }
        }
    )
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

private suspend fun DragAndDropSourceScope.triggerDrag(
    uris: List<Uri>
) {
    detectDragGesturesAfterLongPress(
        onDragStart = dragStart@{
            if (uris.isEmpty()) return@dragStart
            val mimeTypes = uris.map(Uri::mimetype).distinct().toTypedArray()
            val clipData = ClipData(
                "Drag drop",
                mimeTypes,
                ClipData.Item(android.net.Uri.parse(uris.first().path))
            )
            uris.drop(1).forEach { uri ->
                clipData.addItem(ClipData.Item(android.net.Uri.parse(uri.path)))
            }
            startTransfer(
                DragAndDropTransferData(
                    clipData = clipData,
                    flags = View.DRAG_FLAG_GLOBAL
                )
            )
        },
        onDrag = { _, _ -> }
    )
}