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
import androidx.compose.ui.platform.debugInspectorInfo
import com.tunjid.me.core.utilities.Uri

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

internal interface DragSource {

    val size: Size

    val dragShadowPainter: Painter?

    fun dragInfo(offset: Offset): DragInfo?
}

fun Modifier.dragSource(
    dragShadowPainter: Painter? = null,
    dragStatus: () -> DragStatus,
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "dragSource"
        properties["dragStatus"] = dragStatus
    },
    factory = {
        val node = remember {
            DragDropContainer { start ->
                when (start) {
                    is DragDrop.Drop -> DragDropAction.Reject
                    is DragDrop.Drag -> when (dragStatus()) {
                        DragStatus.Static -> DragDropAction.Reject
                        is DragStatus.Draggable -> DragDropAction.Drag(
                            dragSource = object : DragSource {
                                override val size: Size
                                    // Get the size from the modifier after it measures
                                    get() = this@DragDropContainer.size
                                override val dragShadowPainter: Painter? get() = dragShadowPainter

                                override fun dragInfo(offset: Offset): DragInfo? =
                                    when (val dragStatus = dragStatus()) {
                                        DragStatus.Static -> null
                                        is DragStatus.Draggable -> DragInfo(
                                            uris = dragStatus.uris,
                                            size = this@DragDropContainer.size,
                                            dragShadowPainter = dragShadowPainter
                                        )
                                    }
                            }
                        )
                    }
                }
            }
        }
        this.then(node)
    })