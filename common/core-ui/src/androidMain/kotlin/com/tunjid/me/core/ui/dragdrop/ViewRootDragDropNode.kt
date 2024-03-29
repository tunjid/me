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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.modifier.ModifierLocalMap
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import com.tunjid.me.core.utilities.ContentUri
import com.tunjid.me.core.utilities.RemoteUri
import com.tunjid.me.core.utilities.Uri

fun Modifier.rootDragDropModifier(
    dragTriggers: Set<DragTrigger> = setOf(),
    view: View,
): Modifier = this then RootDragDropElement then composed(
    inspectorInfo = debugInspectorInfo {
        name = "ViewDragDetector"
    },
    factory = {
        val spy = remember(keys = dragTriggers.toTypedArray()) {
            val detector = DragTriggerDetector(
                view = view,
                dragTriggers = dragTriggers,
                dragDroppable = RootDragDropElement.dragDropNode
            )
            motionEventSpy(detector::onMotionEvent)
        }

        LaunchedEffect(true) {
            view.setOnDragListener(
                dragListener(
                    dragDroppable = RootDragDropElement.dragDropNode
                )
            )
        }

        this.then(spy)
    })

private object RootDragDropElement : ModifierNodeElement<RootDragDropNode>() {

    private val rootDragDropNode = RootDragDropNode()
    val dragDropNode = rootDragDropNode.dragDropNode

    override fun create() = rootDragDropNode

    override fun update(node: RootDragDropNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "RootDragDropNode"
    }

    override fun hashCode(): Int = 0

    override fun equals(other: Any?): Boolean = this === other
}

internal actual class RootDragDropNode : DelegatingNode(),
    ModifierLocalModifierNode,
    GlobalPositionAwareModifierNode {

    internal val dragDropNode: DragDropNode = delegate(rootDragDropNode())
    override val providedValues: ModifierLocalMap = dragDropNode.providedValues

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) =
        dragDropNode.onGloballyPositioned(coordinates)
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