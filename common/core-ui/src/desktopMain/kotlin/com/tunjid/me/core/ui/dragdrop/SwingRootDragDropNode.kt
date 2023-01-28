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
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.modifier.ModifierLocalMap
import androidx.compose.ui.modifier.ModifierLocalNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.modifierElementOf
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.tunjid.me.core.utilities.FileUri
import com.tunjid.me.core.utilities.Uri
import java.awt.Cursor
import java.awt.Point
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.*
import java.io.File
import java.io.Serializable
import java.awt.dnd.DragSource as AwtDragSource
import java.awt.dnd.DropTarget as AwtDropTarget

fun Modifier.rootDragDropModifier(
    rootDragDropNode: RootDragDropNode,
): Modifier = this then modifierElementOf(
    create = { rootDragDropNode },
    definitions = {}
)

actual class RootDragDropNode(
    density: Float,
    window: ComposeWindow,
) : DelegatingNode(),
    ModifierLocalNode,
    GlobalPositionAwareModifierNode {

    private val dragDropNode: DragDropNode = delegated { rootDragDropNode() }

    init {
        window.contentPane.dropTarget = AwtDropTarget().apply {
            addDropTargetListener(
                dropTargetListener(
                    dragDropNode = dragDropNode,
                    density = density
                )
            )
        }

        AwtDragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
            window,
            DnDConstants.ACTION_COPY,
            dragGestureListener(
                dragDroppable = dragDropNode,
                density = density
            )
        )
    }

    override val providedValues: ModifierLocalMap = dragDropNode.providedValues

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) =
        dragDropNode.onGloballyPositioned(coordinates)
}

private fun dropTargetListener(
    dragDropNode: DragDroppable,
    density: Float,
) = object : DropTargetListener {
    override fun dragEnter(dtde: DropTargetDragEvent?) {
        if (dtde == null) return
        dragDropNode.onStarted(
            dtde.startDragMimeTypes(),
            Offset(
                dtde.location.x * density,
                dtde.location.y * density
            )
        )
        dragDropNode.onEntered()
    }

    override fun dragOver(dtde: DropTargetDragEvent?) {
        if (dtde == null) return
        dragDropNode.onMoved(
            Offset(
                dtde.location.x * density,
                dtde.location.y * density
            )
        )
    }

    override fun dropActionChanged(dtde: DropTargetDragEvent?) = Unit

    override fun dragExit(dte: DropTargetEvent?) {
        dragDropNode.onExited()
        dragDropNode.onEnded()
    }

    override fun drop(dtde: DropTargetDropEvent?) {
        if (dtde == null) return dragDropNode.onEnded()

        dtde.acceptDrop(DnDConstants.ACTION_REFERENCE)
        dtde.dropComplete(
            dragDropNode.onDropped(
                dtde.endDropUris(),
                Offset(
                    dtde.location.x * density,
                    dtde.location.y * density
                )
            )
        )
        dragDropNode.onEnded()
    }
}

private fun dragGestureListener(
    dragDroppable: DragDroppable,
    density: Float,
) = DragGestureListener { event: DragGestureEvent ->
    val offset = Offset(
        x = event.dragOrigin.x * density,
        y = event.dragOrigin.y * density
    )

    println("Dragging")
    val dragInfo = dragDroppable.dragInfo(offset) ?: return@DragGestureListener

    if (dragInfo.uris.isNotEmpty()) when (val shadowPainter = dragInfo.dragShadowPainter) {
        null -> event.startDrag(
            /* dragCursor = */ Cursor.getDefaultCursor(),
            /* transferable = */ dragInfo.transferable()
        )

        else -> event.startDrag(
            /* dragCursor = */ Cursor.getDefaultCursor(),
            /* dragImage = */ shadowPainter.toAwtImage(
                density = Density(density),
                layoutDirection = LayoutDirection.Ltr,
                size = dragInfo.size
            ),
            /* imageOffset = */ Point(-dragInfo.size.width.toInt() / 2, -dragInfo.size.height.toInt() / 2),
            /* transferable = */ dragInfo.transferable(),
            /* dsl = */ object : DragSourceAdapter() {

            }
        )
    }
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

private fun DragInfo.transferable() = object : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> =
        arrayOf(ArrayListFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean =
        flavor == ArrayListFlavor

    override fun getTransferData(flavor: DataFlavor?): Any =
        uris.toSerializableList()
}

private fun List<Uri>.toSerializableList() = java.util.ArrayList(
    map { SerializableUri(path = it.path, mimetype = it.mimetype) }
)

private class SerializableUri(
    override val path: String,
    override val mimetype: String,
) : Uri, Serializable