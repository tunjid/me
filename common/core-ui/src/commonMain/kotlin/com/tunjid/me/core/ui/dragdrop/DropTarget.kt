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

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.debugInspectorInfo
import com.tunjid.me.core.utilities.Uri

interface DropTarget {
    fun onStarted(mimeTypes: Set<String>, position: Offset): Boolean
    fun onEntered()
    fun onMoved(position: Offset) {}
    fun onExited()
    fun onDropped(uris: List<Uri>, position: Offset): Boolean
    fun onEnded()
}

fun Modifier.dropTarget(
    onStarted: (mimeTypes: Set<String>, Offset) -> Boolean,
    onEntered: () -> Unit = { },
    onMoved: (position: Offset) -> Unit = {},
    onExited: () -> Unit = { },
    onDropped: (uris: List<Uri>, position: Offset) -> Boolean,
    onEnded: () -> Unit = {},
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "dropTarget"
        properties["onDragStarted"] = onStarted
    },
    factory = {
        val node = remember {
            DragDropContainer { start ->
                when (start) {
                    is DragDrop.Drag -> DragDropAction.Reject
                    is DragDrop.Drop -> when (
                        onStarted(start.mimeTypes, start.offset)
                    ) {
                        false -> DragDropAction.Reject
                        true -> DragDropAction.Drop(
                            object : DropTarget {
                                override fun onStarted(mimeTypes: Set<String>, position: Offset): Boolean = onStarted(
                                    mimeTypes,
                                    position
                                )

                                override fun onEntered() = onEntered()

                                override fun onMoved(position: Offset) = onMoved(position)

                                override fun onExited() = onExited()

                                override fun onDropped(uris: List<Uri>, position: Offset): Boolean = onDropped(
                                    uris,
                                    position
                                )

                                override fun onEnded() = onEnded()
                            }
                        )
                    }
                }
            }
        }
        this.then(node)
    })

