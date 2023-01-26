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

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.modifier.ModifierLocalMap
import androidx.compose.ui.modifier.ModifierLocalNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.modifierElementOf
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
): Modifier = this then modifierElementOf(
    params = listOf(onStarted, onEntered, onMoved, onExited, onDropped, onEnded),
    create = {
        DropTargetNode(
            onStarted = onStarted,
            onEntered = onEntered,
            onMoved = onMoved,
            onExited = onExited,
            onDropped = onDropped,
            onEnded = onEnded
        )
    },
    update = { dropTarget ->
        dropTarget.onStarted = onStarted
        dropTarget.onEntered = onEntered
        dropTarget.onMoved = onMoved
        dropTarget.onExited = onExited
        dropTarget.onDropped = onDropped
        dropTarget.onEnded = onEnded
    },
    definitions = {
        this.name = "dropTarget"
        properties["onDragStarted"] = onStarted
        properties["onEntered"] = onEntered
        properties["onMoved"] = onMoved
        properties["onExited"] = onExited
        properties["onDropped"] = onDropped
        properties["onEnded"] = onEnded
    },
)

private class DropTargetNode(
    var onStarted: (mimeTypes: Set<String>, Offset) -> Boolean,
    var onEntered: () -> Unit,
    var onMoved: (position: Offset) -> Unit,
    var onExited: () -> Unit,
    var onDropped: (uris: List<Uri>, position: Offset) -> Boolean,
    var onEnded: () -> Unit,
) : DelegatingNode(),
    ModifierLocalNode,
    GlobalPositionAwareModifierNode,
    DropTarget {

    private val dragDropNode = delegated {
        DragDropNode { start ->
            when (start) {
                is DragDrop.Drag -> DragDropAction.Reject
                is DragDrop.Drop -> when (
                    onStarted(start.mimeTypes, start.offset)
                ) {
                    false -> DragDropAction.Reject
                    true -> DragDropAction.Drop(
                        dropTarget = this@DropTargetNode
                    )
                }
            }
        }
    }

    override val providedValues: ModifierLocalMap = dragDropNode.providedValues


    private var coordinates: LayoutCoordinates? = null

    override fun onStarted(mimeTypes: Set<String>, position: Offset): Boolean = onStarted.invoke(
        mimeTypes,
        coordinates?.windowToLocal(position) ?: position
    )

    override fun onEntered() = onEntered.invoke()

    override fun onMoved(position: Offset) = onMoved.invoke(
        coordinates?.windowToLocal(position) ?: position
    )

    override fun onExited() = onExited.invoke()

    override fun onDropped(uris: List<Uri>, position: Offset): Boolean = onDropped.invoke(
        uris,
        coordinates?.windowToLocal(position) ?: position
    )

    override fun onEnded() = onEnded.invoke()

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        dragDropNode.onGloballyPositioned(coordinates)
        this.coordinates = coordinates
    }
}

