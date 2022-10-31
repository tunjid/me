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
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.*
import java.io.File
import java.awt.dnd.DropTarget as AwtDropTarget

actual class PlatformDropTargetModifier(
    density: Float,
    window: ComposeWindow,
) : DropTargetModifier by dropTargetModifier() {
    init {
        val awtDropTarget = AwtDropTarget()
        awtDropTarget.addDropTargetListener(
            dropTargetListener(
                dropTargetModifier = this,
                density = density
            )
        )
        window.contentPane.dropTarget = awtDropTarget
    }
}

private fun dropTargetListener(
    dropTargetModifier: DropTargetModifier,
    density: Float
) = object : DropTargetListener {
    override fun dragEnter(dtde: DropTargetDragEvent?) {
        if (dtde == null) return
        dropTargetModifier.onDragStarted(
            listOf(),
            Offset(
                dtde.location.x * density,
                dtde.location.y * density
            )
        )
        dropTargetModifier.onDragEntered()
    }

    override fun dragOver(dtde: DropTargetDragEvent?) {
        if (dtde == null) return
        dropTargetModifier.onDragMoved(
            Offset(
                dtde.location.x * density,
                dtde.location.y * density
            )
        )
    }

    override fun dropActionChanged(dtde: DropTargetDragEvent?) = Unit

    override fun dragExit(dte: DropTargetEvent?) {
        dropTargetModifier.onDragExited()
        dropTargetModifier.onDragEnded()
    }

    override fun drop(dtde: DropTargetDropEvent?) {
        if (dtde == null) return dropTargetModifier.onDragEnded()

        dtde.acceptDrop(DnDConstants.ACTION_REFERENCE)
        dtde.dropComplete(
            dropTargetModifier.onDropped(
                dtde.fileUris(),
                Offset(
                    dtde.location.x * density,
                    dtde.location.y * density
                )
            )
        )
        dropTargetModifier.onDragEnded()
    }
}

private fun DropTargetDropEvent.fileUris(): List<Uri> = transferable
    .getTransferData(DataFlavor.javaFileListFlavor)
    .let { it as? List<*> ?: listOf<File>() }
    .filterIsInstance<File>()
    .map(::FileUri)
