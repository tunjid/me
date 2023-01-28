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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.modifier.ModifierLocalMap
import androidx.compose.ui.modifier.ModifierLocalNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.modifierElementOf
import com.tunjid.me.core.utilities.ContentUri
import com.tunjid.me.core.utilities.RemoteUri
import com.tunjid.me.core.utilities.Uri

fun Modifier.rootDragDropModifier(
    dragTriggers: Set<DragTrigger> = setOf(),
    rootDragDropNode: RootDragDropNode,
): Modifier = this then modifierElementOf(
    params = dragTriggers,
    create = {
        rootDragDropNode.dragTriggers = dragTriggers
        rootDragDropNode
    },
    update = { it.dragTriggers = dragTriggers },
    definitions = {}
)

actual class RootDragDropNode : DelegatingNode(),
    ModifierLocalNode,
    GlobalPositionAwareModifierNode,
    View.OnAttachStateChangeListener {

    private val dragDropNode: DragDropNode = delegated { rootDragDropNode() }
    private lateinit var dragGestureDetector: DragTriggerDetector
    internal var dragTriggers = setOf<DragTrigger>()
        set(value) {
            field = value
            if (::dragGestureDetector.isInitialized) dragGestureDetector.dragTriggers = value
        }

    override val providedValues: ModifierLocalMap = dragDropNode.providedValues

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) =
        dragDropNode.onGloballyPositioned(coordinates)

    override fun onViewAttachedToWindow(view: View) {
        check(view is ContentView)
        view.onAttached(
            dragListener = dragListener(
                dragDroppable = dragDropNode
            ),
            touchListener = DragTriggerDetector(
                view = view,
                dragTriggers = dragTriggers,
                dragDroppable = dragDropNode
            ).also(::dragGestureDetector::set)
        )
    }

    override fun onViewDetachedFromWindow(view: View) = Unit
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