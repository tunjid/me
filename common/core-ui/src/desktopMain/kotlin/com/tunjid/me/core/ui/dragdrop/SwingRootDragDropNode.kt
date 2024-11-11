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

import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferAction
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.DragAndDropTransferable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import com.tunjid.me.core.utilities.FileUri
import com.tunjid.me.core.utilities.Uri
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.io.File
import java.io.Serializable

actual fun Modifier.dragSource(
    drawDragDecoration: (DrawScope.() -> Unit)?,
    uris: List<Uri>,
): Modifier = dragAndDropSource(
    drawDragDecoration = {
        drawDragDecoration?.invoke(this)
    },
    block = {
        detectDragGestures(
            onDragStart = dragStart@{
                if (uris.isEmpty()) return@dragStart
                startTransfer(
                    DragAndDropTransferData(
                        transferable = DragAndDropTransferable(
                            transferable = uris.transferable(),
                        ),
                        supportedActions = listOf(
                            DragAndDropTransferAction.Copy,
                            DragAndDropTransferAction.Move,
                            DragAndDropTransferAction.Link,
                        ),
                        dragDecorationOffset = it,
                        onTransferCompleted = { action ->
                            when (action) {
                                null -> println("Transfer aborted")
                                DragAndDropTransferAction.Copy -> println("Copied")
                                DragAndDropTransferAction.Move -> println("Moved")
                                DragAndDropTransferAction.Link -> println("Linked")
                            }
                        }
                    )
                )
            },
            onDrag = { _, _ -> },
        )
    }
)

@Composable
actual fun Modifier.dropTarget(
    onStarted: (mimeTypes: Set<String>, Offset) -> Boolean,
    onEntered: () -> Unit,
    onMoved: (position: Offset) -> Unit,
    onExited: () -> Unit,
    onDropped: (uris: List<Uri>, position: Offset) -> Boolean,
    onEnded: () -> Unit,
): Modifier {
    val updatedOnStart by rememberUpdatedState(onStarted)
    val updatedOnEntered by rememberUpdatedState(onEntered)
    val updatedOnMove by rememberUpdatedState(onMoved)
    val updatedOnExit by rememberUpdatedState(onExited)
    val updatedOnDrop by rememberUpdatedState(onDropped)
    val updatedOnEnded by rememberUpdatedState(onEnded)

    val density = LocalDensity.current.density
    return dragAndDropTarget(
        shouldStartDragAndDrop = shouldStartDragAndDrop@{ startEvent ->
            val nativeEvent = startEvent.nativeEvent as? DropTargetDragEvent
                ?: return@shouldStartDragAndDrop false
            updatedOnStart(
                nativeEvent.startDragMimeTypes(),
                Offset(
                    x = nativeEvent.location.x * density,
                    y = nativeEvent.location.y * density,
                )
            )
        },
        target = remember {
            object : DragAndDropTarget {
                override fun onEntered(event: DragAndDropEvent) {
                    updatedOnEntered()
                }

                override fun onMoved(event: DragAndDropEvent) {
                    val nativeEvent = event.nativeEvent as? DropTargetDragEvent ?: return
                    updatedOnMove(
                        Offset(
                            x = nativeEvent.location.x * density,
                            y = nativeEvent.location.y * density,
                        )
                    )
                }

                override fun onExited(event: DragAndDropEvent) {
                    updatedOnExit()
                }

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    val nativeEvent = event.nativeEvent as? DropTargetDropEvent ?: return false
                    return updatedOnDrop(
                        nativeEvent.endDropUris(),
                        Offset(
                            x = nativeEvent.location.x * density,
                            y = nativeEvent.location.y * density,
                        )
                    )
                }

                override fun onEnded(event: DragAndDropEvent) {
                    updatedOnEnded()
                }
            }
        }
    )
}

private fun DropTargetDragEvent.startDragMimeTypes(): Set<String> =
    transferable
        .transferDataFlavors
        .filter(transferable::isDataFlavorSupported)
        .map(::uris)
        .flatten()
        .map(Uri::mimetype)
        .toSet()

private fun DropTargetDropEvent.endDropUris(): List<Uri> =
    transferable
        .transferDataFlavors
        .filter(transferable::isDataFlavorSupported)
        .map(::uris)
        .flatten()

private fun DropTargetEvent.uris(dataFlavor: DataFlavor?): List<Uri> {
    val transferable = when (this) {
        is DropTargetDragEvent -> transferable
        is DropTargetDropEvent -> transferable
        else -> null
    } ?: return emptyList()

    return when (dataFlavor) {
        DataFlavor.javaFileListFlavor ->
            transferable.getTransferData(dataFlavor)
                .let { it as? List<*> ?: listOf<File>() }
                .filterIsInstance<File>()
                .map(::FileUri)

        ArrayListFlavor ->
            transferable.getTransferData(dataFlavor)
                .let { it as? List<*> ?: listOf<SerializableUri>() }
                .filterIsInstance<SerializableUri>()

        else -> listOf()
    }
}

val ArrayListFlavor = DataFlavor(
    java.util.ArrayList::class.java,
    "ArrayList"
)

internal fun List<Uri>.transferable() = object : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> =
        arrayOf(ArrayListFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean =
        flavor == ArrayListFlavor

    override fun getTransferData(flavor: DataFlavor?): Any =
        toSerializableList()
}

private fun List<Uri>.toSerializableList() = java.util.ArrayList(
    map { SerializableUri(path = it.path, mimetype = it.mimetype) }
)

private class SerializableUri(
    override val path: String,
    override val mimetype: String,
) : Uri, Serializable