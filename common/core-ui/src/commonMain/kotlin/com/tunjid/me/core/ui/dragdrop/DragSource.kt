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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.OnGloballyPositionedModifier
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import com.tunjid.me.core.utilities.Uri

internal interface DragSource {

    val size: IntSize

    val dragShadowPainter: Painter?

    fun dragInfo(offset: Offset): DragInfo?
}

sealed class DragStatus {

    internal object Static : DragStatus()

    internal data class Draggable(val uris: List<Uri>) : DragStatus()


    companion object {
        fun static(): DragStatus = Static
        fun draggable(uris: List<Uri>): DragStatus = Draggable(uris)
    }
}

internal data class DragInfo(
    val size: Size,
    val uris: List<Uri>,
    val dragShadowPainter: Painter?,
)

fun Modifier.dragSource(
    dragShadowPainter: Painter? = null,
    dragStatus: () -> DragStatus,
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "dragSource"
        properties["dragShadowPainter"] = dragShadowPainter
        properties["dragStatus"] = dragStatus
    },
    factory = {
        val dragSource = remember {
            MutableDragSource(
                dragShadowPainter = dragShadowPainter,
                dragStatus = dragStatus
            )
        }
        val node = remember {
            DragDropContainer { start ->
                when (start) {
                    is DragDrop.Drop -> DragDropAction.Reject
                    is DragDrop.Drag -> when (dragSource.dragStatus()) {
                        DragStatus.Static -> DragDropAction.Reject
                        is DragStatus.Draggable -> DragDropAction.Drag(
                            dragSource = dragSource
                        )
                    }
                }
            }
        }

        dragSource.dragShadowPainter = dragShadowPainter
        dragSource.dragStatus = dragStatus

        this.then(node).then(dragSource)
    })

private class MutableDragSource(
    override var dragShadowPainter: Painter?,
    var dragStatus: () -> DragStatus,
) : DragSource, OnGloballyPositionedModifier {

    private var coordinates: LayoutCoordinates? = null
    override val size: IntSize
        get() = when (val coordinates = coordinates) {
            null -> IntSize.Zero
            else -> coordinates.size
        }

    override fun dragInfo(offset: Offset): DragInfo? =
        when (val dragStatus = dragStatus()) {
            DragStatus.Static -> null
            is DragStatus.Draggable -> DragInfo(
                uris = dragStatus.uris,
                size = size.toSize(),
                dragShadowPainter = dragShadowPainter
            )
        }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }
}