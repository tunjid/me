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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.modifier.ModifierLocalMap
import androidx.compose.ui.modifier.ModifierLocalNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import com.tunjid.me.core.utilities.Uri

internal sealed interface DragSource : DragOrDrop {

    val size: IntSize

    val dragShadowPainter: Painter?

    fun dragInfo(offset: Offset): DragInfo?
}

internal data class DragInfo(
    val size: Size,
    val uris: List<Uri>,
    val dragShadowPainter: Painter?,
)

fun Modifier.dragSource(
    dragShadowPainter: Painter? = null,
    uris: List<Uri>,
): Modifier = this then DragSourceElement(
    dragShadowPainter = dragShadowPainter,
    uris = uris
)

private data class DragSourceElement(
    /**
     * Optional painter to draw the drag shadow for the UI.
     * If not provided, the system default will be used.
     */
    val dragShadowPainter: Painter? = null,
    /**
     * A items to be transferred. If empty, a drag gesture will not be started.
     */
    val uris: List<Uri>,
) : ModifierNodeElement<DragSourceNode>() {
    override fun create() = DragSourceNode(
        dragShadowPainter = dragShadowPainter,
        uris = uris
    )

    override fun update(node: DragSourceNode) = node.apply {
        dragShadowPainter = this@DragSourceElement.dragShadowPainter
        uris = this@DragSourceElement.uris
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "dragSource"
        properties["dragShadowPainter"] = dragShadowPainter
        properties["dragStatus"] = uris
    }
}

private class DragSourceNode(
    override var dragShadowPainter: Painter?,
    var uris: List<Uri>,
) : DelegatingNode(),
    ModifierLocalNode,
    GlobalPositionAwareModifierNode,
    DragSource {

    private val dragDropNode = delegated {
        DragDropNode { start ->
            when (start) {
                is DragOrDropStart.Drop -> null
                is DragOrDropStart.Drag -> when (uris) {
                    emptyList<Uri>() -> null
                    else -> this@DragSourceNode
                }
            }
        }
    }

    override val providedValues: ModifierLocalMap = dragDropNode.providedValues

    override val size: IntSize get() = dragDropNode.size

    override fun dragInfo(offset: Offset): DragInfo? =
        when (uris) {
            emptyList<Uri>() -> null
            else -> DragInfo(
                uris = uris,
                size = size.toSize(),
                dragShadowPainter = dragShadowPainter
            )
        }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) =
        dragDropNode.onGloballyPositioned(coordinates)
}