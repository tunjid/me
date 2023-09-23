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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.me.core.model.ArchiveFile
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.imageMimetypes
import com.tunjid.me.core.model.miscMimeTypes
import com.tunjid.me.core.ui.dragdrop.dragSource
import com.tunjid.me.core.ui.dragdrop.dropTarget
import com.tunjid.me.core.ui.maxSize
import com.tunjid.me.core.ui.rememberAsyncRasterPainter
import com.tunjid.me.core.utilities.RemoteUri
import com.tunjid.me.feature.LocalScreenStateHolderCache
import com.tunjid.me.scaffold.lifecycle.component1
import com.tunjid.me.scaffold.lifecycle.component2
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.permissions.Permission
import com.tunjid.tiler.compose.PivotedTilingEffect
import kotlinx.serialization.Serializable

enum class FileType(val mimeTypes: Set<String>) {
    Image(imageMimetypes),
    Misc(miscMimeTypes)
}

@Serializable
data class ArchiveFilesRoute(
    override val id: String,
    val kind: ArchiveKind,
    val archiveId: ArchiveId,
    val dndEnabled: Boolean = false,
    val fileType: FileType = FileType.Image
) : AppRoute {
    @Composable
    override fun Render(modifier: Modifier) {
        ArchiveFilesScreen(
            modifier = modifier,
            stateHolder = LocalScreenStateHolderCache.current.screenStateHolderFor(this),
        )
    }
}

@Composable
internal fun ArchiveFilesScreen(
    modifier: Modifier,
    stateHolder: ArchiveFilesStateHolder,
) {
    val (state, actions) = stateHolder
    val gridState = rememberLazyGridState()

    if (state.isInPrimaryNav) GlobalUi()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        FilesGrid(
            dndEnabled = state.dndEnabled,
            fileType = state.fileType,
            files = state.files,
            lazyGridState = gridState,
            actions = actions
        )
        FilesDrop(
            dndEnabled = state.dndEnabled,
            dragLocation = state.dragLocation,
            hasStoragePermissions = state.hasStoragePermissions,
            actions = actions
        )
        UploadInfo(state.uploadInfo)

        gridState.PivotedTilingEffect(
            items = state.files,
            onQueryChanged = { query ->
                actions(
                    Action.Fetch.LoadAround(
                        query = query ?: state.startQuery()
                    )
                )
            }
        )
    }
}

@Composable
private fun FilesGrid(
    dndEnabled: Boolean,
    lazyGridState: LazyGridState,
    fileType: FileType,
    files: List<ArchiveFile>,
    actions: (Action.Fetch.ColumnSizeChanged) -> Unit,
) {
    LazyVerticalGrid(
        state = lazyGridState,
        columns = when (fileType) {
            FileType.Image -> GridCells.Adaptive(100.dp)
            FileType.Misc -> GridCells.Fixed(count = 1)
        },
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(
            items = files,
            key = ArchiveFile::url,
            span = {
                actions(Action.Fetch.ColumnSizeChanged(maxLineSpan))
                GridItemSpan(1)
            },
            itemContent = { archiveFile ->
                when (fileType) {
                    FileType.Image -> ImageFile(
                        modifier = Modifier.animateItemPlacement(),
                        dndEnabled = dndEnabled,
                        archiveFile = archiveFile
                    )

                    FileType.Misc -> TextFile(
                        modifier = Modifier.animateItemPlacement(),
                        archiveFile = archiveFile
                    )
                }
            }
        )
    }
}

@Composable
private fun FilesDrop(
    dndEnabled: Boolean,
    dragLocation: DragLocation,
    hasStoragePermissions: Boolean,
    actions: (Action) -> Unit,
) {
    val borderColor by animateColorAsState(
        when (dragLocation) {
            DragLocation.Inside -> MaterialTheme.colorScheme.primaryContainer
            DragLocation.Outside -> MaterialTheme.colorScheme.errorContainer
            DragLocation.Inactive -> Color.Transparent
        }
    )
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = 2.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium,
            )
            .run {
                if (!dndEnabled) this
                else dropTarget(
                    onStarted = { _, _ ->
                        val (action, acceptedDrag) = when (hasStoragePermissions) {
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
                )
            }
    )
}

@Composable
private fun ImageFile(
    modifier: Modifier = Modifier,
    dndEnabled: Boolean,
    archiveFile: ArchiveFile,
) {
    BoxWithConstraints(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
            .aspectRatio(1f)
    ) {
        val imagePainter = rememberAsyncRasterPainter(
            imageUri = archiveFile.url,
            size = maxSize()
        )
        if (imagePainter != null) Image(
            painter = imagePainter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = if (!dndEnabled) Modifier
            else Modifier.dragSource(
                dragShadowPainter = imagePainter,
                uris = listOf(
                    RemoteUri(
                        path = archiveFile.url,
                        mimetype = archiveFile.mimeType,
                    )
                )
            )
        )
    }
}

@Composable
private fun TextFile(
    modifier: Modifier = Modifier,
    archiveFile: ArchiveFile,
) {
    Text(
        modifier = modifier,
        text = archiveFile.url
    )
}

@Composable
fun BoxScope.UploadInfo(info: UploadInfo) {
    val message = when (info) {
        UploadInfo.None -> return
        is UploadInfo.Message -> info.message
        is UploadInfo.Progress -> info.message
    }
    val progress = if (info is UploadInfo.Progress) info.progress else null

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth(.8f)
            .padding(8.dp)
            .align(Alignment.BottomCenter),
    ) {
        Column(
            modifier = Modifier.animateContentSize()
        ) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = message
            )
            if (progress != null) LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                progress = progress,
            )
        }

    }
}