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
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.modifierElementOf

expect class RootDragDropNode : DelegatingNode

internal interface DragDroppable : DragSource, DropTarget

/**
 * Root level [Modifier.Node], it always rejects leaving acceptance to its children
 */
internal fun rootDragDropNode() = DragDropNode(
    onDragDropStarted = { _ -> DragDropAction.Reject }
)

internal interface DragDropParent {

    val children: List<DragDropChild>

    fun registerChild(child: DragDropChild)
    fun unregisterChild(child: DragDropChild)
}

internal interface DragDropChild : DragDroppable, DragDropParent {
    val coordinates: LayoutCoordinates?
}

internal val DragDropChild.area
    get() =
        size.width * size.height

internal sealed class DragDropAction {
    object Reject : DragDropAction()

    data class Drag(val dragSource: DragSource) : DragDropAction()

    data class Drop(val dropTarget: DropTarget) : DragDropAction()


    internal val target: DropTarget?
        get() = when (this) {
            Reject -> null
            is Drag -> null
            is Drop -> dropTarget
        }

    internal val source: DragSource?
        get() = when (this) {
            Reject -> null
            is Drop -> null
            is Drag -> dragSource
        }
}

internal sealed class DragDrop {
    data class Drag(
        val offset: Offset,
    ) : DragDrop()

    data class Drop(
        val mimeTypes: Set<String>,
        val offset: Offset,
    ) : DragDrop()
}
