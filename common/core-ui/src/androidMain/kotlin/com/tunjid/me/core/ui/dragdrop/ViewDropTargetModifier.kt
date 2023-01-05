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

import android.view.DragEvent
import android.view.View
import androidx.compose.ui.geometry.Offset
import com.tunjid.me.core.utilities.ClipItemUri
import com.tunjid.me.core.utilities.Uri

actual class PlatformDragDropModifier(
    view: View,
) : DragDropModifier by rootDragDropModifier() {
    init {
        view.setOnDragListener(dragListener(this))
    }
}

fun dragListener(
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
                uris = event.clipItemUris(),
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

private fun DragEvent.clipItemUris(): List<Uri> = with(clipData) {
    0.until(itemCount).map { itemIndex ->
        with(description) {
            0.until(mimeTypeCount).map { mimeTypeIndex ->
                ClipItemUri(
                    item = getItemAt(itemIndex),
                    mimeType = getMimeType(mimeTypeIndex)
                )
            }
        }
    }
        .flatten()
}