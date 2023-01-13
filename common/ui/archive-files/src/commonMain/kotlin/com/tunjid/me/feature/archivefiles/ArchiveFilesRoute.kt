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

package com.tunjid.me.feature.archivefiles

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.me.core.model.ArchiveFile
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.ui.asyncRasterPainter
import com.tunjid.me.core.ui.dragdrop.DragStatus
import com.tunjid.me.core.ui.dragdrop.dragSource
import com.tunjid.me.core.ui.dragdrop.dropTarget
import com.tunjid.me.core.ui.maxSize
import com.tunjid.me.core.utilities.RemoteUri
import com.tunjid.me.feature.LocalScreenStateHolderCache
import com.tunjid.me.scaffold.lifecycle.toActionableState
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.permissions.Permission
import kotlinx.serialization.Serializable

@Serializable
data class ArchiveFilesRoute(
    override val id: String,
    val kind: ArchiveKind,
    val archiveId: ArchiveId,
) : AppRoute {
    @Composable
    override fun Render() {
        ArchiveFilesScreen(
            stateHolder = LocalScreenStateHolderCache.current.screenStateHolderFor(this),
        )
    }
}

@Composable
private fun ArchiveFilesScreen(
    stateHolder: ArchiveFilesStateHolder,
) {
    val screenUiState by stateHolder.toActionableState()
    val (state, actions) = screenUiState

    if (state.isInMainNav) GlobalUi(
        state = state,
        onAction = actions
    )

    val borderColor by animateColorAsState(
        when (state.dragLocation) {
            DragLocation.Inside -> MaterialTheme.colorScheme.primaryContainer
            DragLocation.Outside -> MaterialTheme.colorScheme.errorContainer
            DragLocation.Inactive -> Color.Transparent
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = 2.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium,
            )
            .dropTarget(
                onStarted = { _, _ ->
                    val (action, acceptedDrag) = when (state.hasStoragePermissions) {
                        true -> Action.Drag(location = DragLocation.Outside) to true
                        false -> Action.RequestPermission(Permission.ReadExternalStorage) to false
                    }
                    actions(action)
                    acceptedDrag
                },
                onEntered = { actions(Action.Drag(location = DragLocation.Inside)) },
                onExited = { actions(Action.Drag(location = DragLocation.Outside)) },
                onEnded = { actions(Action.Drag(location = DragLocation.Inactive)) },
                onDropped = { uris, _ ->
                    actions(Action.Drag(location = DragLocation.Inactive))
                    actions(Action.Drop(uris = uris))
                    true
                }
            ),
    ) {
        val uploadProgress = state.uploadProgress
        LazyVerticalGrid(
            columns = GridCells.Adaptive(100.dp)
        ) {
            items(
                items = state.files,
                key = ArchiveFile::url,
                itemContent = { archiveFile ->
                    GalleryItem(archiveFile)
                }
            )
        }
        if (uploadProgress != null) LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = uploadProgress,
        )
    }

}


@Composable
private fun GalleryItem(
    archiveFile: ArchiveFile,
) {
    BoxWithConstraints(modifier = Modifier.aspectRatio(1f)) {
        when (
            val imagePainter = asyncRasterPainter(
                imageUri = archiveFile.url,
                size = maxSize()
            )
        ) {
            null -> Unit
            else -> Image(
                painter = imagePainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.dragSource(
                    dragShadowPainter = imagePainter
                ) {
                    DragStatus.draggable(
                        uris = listOf(
                            RemoteUri(
                                path = archiveFile.url,
                                mimetype = archiveFile.mimeType,
                            )
                        ),
                    )
                }
            )
        }
    }
}
