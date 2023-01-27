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
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.me.core.model.ArchiveFile
import com.tunjid.me.core.model.ArchiveFileQuery
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.ui.rememberAsyncRasterPainter
import com.tunjid.me.core.ui.dragdrop.DragStatus
import com.tunjid.me.core.ui.dragdrop.dragSource
import com.tunjid.me.core.ui.dragdrop.dropTarget
import com.tunjid.me.core.ui.maxSize
import com.tunjid.me.core.utilities.RemoteUri
import com.tunjid.me.feature.LocalScreenStateHolderCache
import com.tunjid.me.scaffold.lifecycle.toActionableState
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.permissions.Permission
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.tiler.queryAtOrNull
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
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
    val gridState = rememberLazyGridState()

    if (state.isInMainNav) GlobalUi()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        FilesGrid(
            files = state.files,
            lazyGridState = gridState,
            actions = actions
        )
        FilesDrop(
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
                        query ?: ArchiveFileQuery(archiveId = state.archiveId)
                    )
                )
            }
        )
    }
}

@Composable
private fun FilesGrid(
    lazyGridState: LazyGridState,
    files: List<ArchiveFile>,
    actions: (Action.Fetch.ColumnSizeChanged) -> Unit,
) {
    LazyVerticalGrid(
        state = lazyGridState,
        columns = GridCells.Adaptive(100.dp),
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
                GalleryItem(
                    modifier = Modifier.animateItemPlacement(),
                    archiveFile = archiveFile
                )
            }
        )
    }
}

@Composable
private fun FilesDrop(
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
            .dropTarget(
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
    )
}

@Composable
private fun GalleryItem(
    modifier: Modifier = Modifier,
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
            modifier = Modifier.dragSource(
                dragShadowPainter = imagePainter,
                dragStatus = {
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
        )
    }
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