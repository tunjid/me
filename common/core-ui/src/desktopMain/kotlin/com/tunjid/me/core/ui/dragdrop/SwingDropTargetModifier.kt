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

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import com.tunjid.me.core.utilities.FileUri
import com.tunjid.me.core.utilities.Uri
import java.awt.Cursor
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.DnDConstants
import java.awt.dnd.DragGestureEvent
import java.awt.dnd.DragGestureListener
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTargetListener
import java.io.File
import java.awt.dnd.DragSource as AwtDragSource
import java.awt.dnd.DropTarget as AwtDropTarget

actual class PlatformDragDropModifier(
    density: Float,
    window: ComposeWindow,
) : DragDropModifier by rootDragDropModifier() {
    init {
        window.contentPane.dropTarget = AwtDropTarget().apply {
            addDropTargetListener(
                dropTargetListener(
                    dragDropModifier = this@PlatformDragDropModifier,
                    density = density
                )
            )
        }

        AwtDragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
            window,
            DnDConstants.ACTION_COPY,
            dragGestureListener(
                dragDropModifier = this@PlatformDragDropModifier,
                density = density
            )
        )
    }
}

private fun dropTargetListener(
    dragDropModifier: DragDropModifier,
    density: Float,
) = object : DropTargetListener {
    override fun dragEnter(dtde: DropTargetDragEvent?) {
        if (dtde == null) return
        dragDropModifier.onDragStarted(
            listOf(),
            Offset(
                dtde.location.x * density,
                dtde.location.y * density
            )
        )
        dragDropModifier.onDragEntered()
    }

    override fun dragOver(dtde: DropTargetDragEvent?) {
        if (dtde == null) return
        dragDropModifier.onDragMoved(
            Offset(
                dtde.location.x * density,
                dtde.location.y * density
            )
        )
    }

    override fun dropActionChanged(dtde: DropTargetDragEvent?) = Unit

    override fun dragExit(dte: DropTargetEvent?) {
        dragDropModifier.onDragExited()
        dragDropModifier.onDragEnded()
    }

    override fun drop(dtde: DropTargetDropEvent?) {
        if (dtde == null) return dragDropModifier.onDragEnded()

        dtde.acceptDrop(DnDConstants.ACTION_REFERENCE)
        dtde.dropComplete(
            dragDropModifier.onDropped(
                dtde.fileUris(),
                Offset(
                    dtde.location.x * density,
                    dtde.location.y * density
                )
            )
        )
        dragDropModifier.onDragEnded()
    }
}

private fun dragGestureListener(
    dragDropModifier: DragDropModifier,
    density: Float,
    ) = DragGestureListener { event: DragGestureEvent ->
    when (val dragStatus = dragDropModifier.dragStatus(
        Offset(
            x = event.dragOrigin.x * density,
            y = event.dragOrigin.y * density
        )
    )) {
        DragStatus.Static -> Unit
        is DragStatus.Draggable -> event.startDrag(
            Cursor(Cursor.HAND_CURSOR),
            object : Transferable {
                override fun getTransferDataFlavors(): Array<DataFlavor> =
                    arrayOf(DataFlavor.javaFileListFlavor)

                override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean =
                    flavor == DataFlavor.javaFileListFlavor

                override fun getTransferData(flavor: DataFlavor?): Any =
//                    dragStatus.uris.filterIsInstance<FileUri>().map(FileUri::file)
                    listOf(File("/Users/adetunjidahunsi/Desktop/scooby.jpeg"))
            }
        )
    }
}

private fun DropTargetDropEvent.fileUris(): List<Uri> = transferable
    .getTransferData(DataFlavor.javaFileListFlavor)
    .let { it as? List<*> ?: listOf<File>() }
    .filterIsInstance<File>()
    .map(::FileUri)
