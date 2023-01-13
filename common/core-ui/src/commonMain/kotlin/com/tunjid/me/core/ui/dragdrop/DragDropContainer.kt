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


import androidx.compose.runtime.RememberObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.OnGloballyPositionedModifier
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.modifier.*
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import com.tunjid.me.core.utilities.Uri

internal class DragDropContainer(
    private val onDragDropStarted: DragSource.(DragDrop) -> DragDropAction,
) : Modifier.Element,
    ModifierLocalConsumer,
    ModifierLocalProvider<DragDropParent?>,
    OnGloballyPositionedModifier,
    RememberObserver,
    DragDropParent,
    DragDropChild,
    DragDropModifier,
    InspectorValueInfo(debugInspectorInfo {
        name = "dropTarget"
        properties["onDragStarted"] = onDragDropStarted
    }) {

    private var parent: DragDropParent? = null
        set(value) {
            if (value != field) {
                field?.unregisterChild(this)
                field = value
                field?.registerChild(this)
            }
        }
    override val children = mutableListOf<DragDropChild>()
    private var coordinates: LayoutCoordinates? = null
    private var activeChild: DragDropChild? = null
    private var currentTarget: DropTarget? = null

    // start ModifierLocalProvider
    override val key: ProvidableModifierLocal<DragDropParent?>
        get() = ModifierLocalDragDropParent

    override val value: DragDropContainer
        get() = this
    // end ModifierLocalProvider

    // start ModifierLocalConsumer
    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) {
        parent = with(scope) { key.current }
    }
    // end ModifierLocalConsumer

    // start RememberObserver
    override fun onRemembered() {}

    override fun onForgotten() {
        parent = null
        currentTarget = null
    }

    override fun onAbandoned() {}
    // end RememberObserver

    // start DropTargetParent
    override fun registerChild(child: DragDropChild) {
        children += child
        // TODO if a drag is in progress, check if we need to send events
    }

    override fun unregisterChild(child: DragDropChild) {
        children -= child
    }
    // end DropTargetParent

    // start DropTargetNode

    override val size: IntSize
        get() = when (val coordinates = coordinates) {
            null -> IntSize.Zero
            else -> coordinates.size
        }

    override fun contains(position: Offset): Boolean {
        val currentCoordinates = coordinates ?: return false
        if (!currentCoordinates.isAttached) return false

        val (width, height) = currentCoordinates.size
        val (x1, y1) = currentCoordinates.positionInRoot()
        val x2 = x1 + width
        val y2 = y1 + height

        return position.x in x1..x2 && position.y in y1..y2
    }

    // end DropTargetNode


    // start DragSource

    override val dragShadowPainter: Painter?
        get() = null

    override fun dragInfo(offset: Offset): DragInfo? {
        coordinates ?: return null

        var smallestDraggedChild: DragDropChild? = null
        depthFirstTraversal { child ->
            if (child != this &&
                child.contains(offset) &&
                child.area < (smallestDraggedChild?.area ?: Int.MAX_VALUE)) {
                smallestDraggedChild = child
            }
        }

        // Attempt to drag the smallest child within the bounds first
        val childDragStatus = smallestDraggedChild?.dragInfo(offset)
        if (childDragStatus != null) return childDragStatus

        // No draggable child, attempt to drag self
        return onDragDropStarted(DragDrop.Drag(offset)).source?.dragInfo(offset)
    }

    // end DragSource

    // start DropTarget
    override fun onStarted(mimeTypes: Set<String>, position: Offset): Boolean {
        coordinates ?: return false

        check(currentTarget == null)
        currentTarget = onDragDropStarted(DragDrop.Drop(mimeTypes, position)).target

        var handledByChild = false

        children.fastForEach { child ->
            handledByChild = handledByChild or child.onStarted(
                mimeTypes = mimeTypes,
                position = position
            )
        }
        return handledByChild || currentTarget != null
    }

    override fun onEntered() {
        currentTarget?.onEntered()
    }

    override fun onMoved(position: Offset) {
        coordinates ?: return
        val currentActiveChild: DragDropChild? = activeChild

        val newChild: DragDropChild? = when (currentActiveChild != null && currentActiveChild.contains(position)) {
            // Moved within child.
            true -> currentActiveChild
            // Position is now outside active child, maybe it entered a different one.
            false -> children.fastFirstOrNull { it.contains(position) }
        }
        when {
            // Left us and went to a child.
            newChild != null && currentActiveChild == null -> {
                currentTarget?.onExited()
                newChild.dispatchEntered(position)
            }
            // Left the child and returned to us.
            newChild == null && currentActiveChild != null -> {
                currentActiveChild.onExited()
                currentTarget?.dispatchEntered(position)
            }
            // Left one child and entered another.
            newChild != currentActiveChild -> {
                currentActiveChild?.onExited()
                newChild?.dispatchEntered(position)
            }
            // Stayed in the same child.
            newChild != null -> newChild.onMoved(position)
            // Stayed in us.
            else -> currentTarget?.onMoved(position)
        }

        this.activeChild = newChild
    }

    override fun onExited() {
//        activeChild?.onDragExited()
//        activeChild = null
        currentTarget?.onExited()
    }

    override fun onDropped(uris: List<Uri>, position: Offset): Boolean =
        when (val currentActiveChild = activeChild) {
            null -> currentTarget?.onDropped(
                uris = uris,
                position = position
            ) ?: false

            else -> currentActiveChild.onDropped(
                uris = uris,
                position = position
            )
        }

    override fun onEnded() {
        children.fastForEach {
            it.onEnded()
        }
        currentTarget?.onEnded()
        currentTarget = null
    }
    // end DropTarget

    // start OnGloballyPositionedModifier
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }
    // end OnGloballyPositionedModifier

}

private fun DropTarget.dispatchEntered(position: Offset) {
    onEntered()
    onMoved(position)
}

private fun DragDropChild.depthFirstTraversal(visitor: (DragDropChild) -> Unit) {
    visitor(this)
    children.fastForEach { child ->
        child.depthFirstTraversal(visitor)
    }
}