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

sealed class DragStatus {
    object Static : DragStatus()

    data class Draggable(val uris: List<Uri>) : DragStatus()
}

interface DragSource {
    fun dragStatus(offset: Offset): DragStatus
}

fun Modifier.dragSource(
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
                            object : DragSource {
                                override fun dragStatus(offset: Offset): DragStatus = dragStatus()
                            }
                        )
                    }
                }
            }
        }
        this.then(node)
    })