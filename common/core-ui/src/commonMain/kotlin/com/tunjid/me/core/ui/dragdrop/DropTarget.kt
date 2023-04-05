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
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import com.tunjid.me.core.utilities.Uri

internal sealed interface DropTarget : DragOrDrop {
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
): Modifier = this then DropTargetElement(
    onStarted = onStarted,
    onEntered = onEntered,
    onMoved = onMoved,
    onExited = onExited,
    onDropped = onDropped,
    onEnded = onEnded
)

private data class DropTargetElement(
    val onStarted: (mimeTypes: Set<String>, Offset) -> Boolean,
    val onEntered: () -> Unit = { },
    val onMoved: (position: Offset) -> Unit = {},
    val onExited: () -> Unit = { },
    val onDropped: (uris: List<Uri>, position: Offset) -> Boolean,
    val onEnded: () -> Unit = {},
) : ModifierNodeElement<DropTargetNode>() {
    override fun create() = DropTargetNode(
        onStarted = onStarted,
        onEntered = onEntered,
        onMoved = onMoved,
        onExited = onExited,
        onDropped = onDropped,
        onEnded = onEnded,
    )

    override fun update(node: DropTargetNode) = node.apply {
        onStarted = this@DropTargetElement.onStarted
        onEntered = this@DropTargetElement.onEntered
        onMoved = this@DropTargetElement.onMoved
        onExited = this@DropTargetElement.onExited
        onDropped = this@DropTargetElement.onDropped
        onEnded = this@DropTargetElement.onEnded
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "dropTarget"
        properties["onDragStarted"] = onStarted
        properties["onEntered"] = onEntered
        properties["onMoved"] = onMoved
        properties["onExited"] = onExited
        properties["onDropped"] = onDropped
        properties["onEnded"] = onEnded
    }
}

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
                is DragOrDropStart.Drag -> null
                is DragOrDropStart.Drop -> when {
                    onStarted(start.mimeTypes, start.offset) -> this@DropTargetNode
                    else -> null
                }
            }
        }
    }

    override val providedValues: ModifierLocalMap = dragDropNode.providedValues

    override fun onStarted(mimeTypes: Set<String>, position: Offset): Boolean = onStarted.invoke(
        mimeTypes,
        dragDropNode.coordinates?.windowToLocal(position) ?: position
    )

    override fun onEntered() = onEntered.invoke()

    override fun onMoved(position: Offset) = onMoved.invoke(
        dragDropNode.coordinates?.windowToLocal(position) ?: position
    )

    override fun onExited() = onExited.invoke()

    override fun onDropped(uris: List<Uri>, position: Offset): Boolean = onDropped.invoke(
        uris,
        dragDropNode.coordinates?.windowToLocal(position) ?: position
    )

    override fun onEnded() = onEnded.invoke()

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) =
        dragDropNode.onGloballyPositioned(coordinates)
}

