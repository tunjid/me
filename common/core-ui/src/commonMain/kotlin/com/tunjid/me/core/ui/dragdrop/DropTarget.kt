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
    fun onDragStarted(uris: List<Uri>, position: Offset): Boolean
    fun onDragEntered()
    fun onDragMoved(position: Offset) {}
    fun onDragExited()
    fun onDropped(uris: List<Uri>, position: Offset): Boolean
    fun onDragEnded()
}

internal interface DropTargetParent {
    fun registerChild(child: DropTargetChild)
    fun unregisterChild(child: DropTargetChild)
}

internal interface DropTargetChild : DropTarget {
    fun contains(position: Offset): Boolean
}

fun Modifier.dropTarget(
    onDragStarted: (uris: List<Uri>, Offset) -> Boolean,
    onDragEntered: () -> Unit = { },
    onDragMoved: (position: Offset) -> Unit = {},
    onDragExited: () -> Unit = { },
    onDropped: (uris: List<Uri>, position: Offset) -> Boolean,
    onDragEnded: () -> Unit = {},
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "dropTarget"
        properties["onDragStarted"] = onDragStarted
    },
    factory = {
        val node = remember {
            DragDropContainer { uris, offset ->
                when (onDragStarted(uris, offset)) {
                    false -> DragDropAction.Reject
                    true -> DragDropAction.Drop(
                        object : DropTarget {
                            override fun onDragStarted(uris: List<Uri>, position: Offset): Boolean = onDragStarted(
                                uris,
                                position
                            )

                            override fun onDragEntered() = onDragEntered()

                            override fun onDragMoved(position: Offset) = onDragMoved(position)

                            override fun onDragExited() = onDragExited()

                            override fun onDropped(uris: List<Uri>, position: Offset): Boolean = onDropped(
                                uris,
                                position
                            )

                            override fun onDragEnded() = onDragEnded()
                        }
                    )
                }
            }
        }
        this.then(node)
    })

