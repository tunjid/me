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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tunjid.me.core.model.imageMimetypes
import com.tunjid.me.core.model.miscMimeTypes
import com.tunjid.me.core.ui.AsyncRasterImage
import com.tunjid.me.core.ui.MediaArgs
import com.tunjid.me.core.ui.dragdrop.dragSource
import com.tunjid.me.core.ui.dragdrop.dropTarget
import com.tunjid.me.core.utilities.RemoteUri
import com.tunjid.me.scaffold.adaptive.routeOf
import com.tunjid.me.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.me.scaffold.permissions.Permission
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.strings.RouteParams

enum class FileType(
    val kind: String,
    val mimeTypes: Set<String>
) {
    Image(
        kind = "image",
        mimeTypes = imageMimetypes
    ),
    Misc(
        kind = "misc",
        mimeTypes = miscMimeTypes
    )
}

fun ArchiveFilesRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams
)

@Composable
internal fun ArchiveFilesScreen(
    movableSharedElementScope: MovableSharedElementScope,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        FilesGrid(
            movableSharedElementScope = movableSharedElementScope,
            dndEnabled = state.dndEnabled,
            fileType = state.fileType,
            files = state.items,
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
            items = state.items,
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
    movableSharedElementScope: MovableSharedElementScope,
    dndEnabled: Boolean,
    lazyGridState: LazyGridState,
    fileType: FileType,
    files: List<FileItem>,
    actions: (Action) -> Unit,
) {
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
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
            key = FileItem::key,
            span = {
                actions(Action.Fetch.ColumnSizeChanged(maxLineSpan))
                GridItemSpan(1)
            },
            itemContent = { fileItem ->
                when (fileType) {
                    FileType.Image -> ImageFile(
                        movableSharedElementScope = movableSharedElementScope,
                        modifier = Modifier,
                        dndEnabled = dndEnabled,
                        fileItem = fileItem,
                        actions = actions
                    )

                    FileType.Misc -> TextFile(
                        modifier = Modifier,
                        fileItem = fileItem
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ImageFile(
    movableSharedElementScope: MovableSharedElementScope,
    modifier: Modifier = Modifier,
    dndEnabled: Boolean,
    fileItem: FileItem,
    actions: (Action) -> Unit,
) {
    var size by remember { mutableStateOf<IntSize?>(null) }
    val sharedElement = movableSharedElementScope.movableSharedElementOf<MediaArgs>(
        key = thumbnailSharedElementKey(fileItem.url)
    ) { args, sharedElementModifier ->
        val imageModifier = sharedElementModifier.onSizeChanged { size = it }

        if (fileItem is FileItem.File) AsyncRasterImage(
            args = args,
            modifier = when {
                !dndEnabled -> imageModifier
                else -> imageModifier.dragSource(
                    dragShadowPainter = ColorPainter(Color.Green),
                    uris = listOf(
                        RemoteUri(
                            path = fileItem.archiveFile.url,
                            mimetype = fileItem.archiveFile.mimeType,
                        )
                    )
                )
            }
        )
    }
    sharedElement(
        MediaArgs(
            url = fileItem.url,
            contentScale = ContentScale.Crop,
        ),
        modifier
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
//            .clickable {
//                val archiveFile = when (fileItem) {
//                    is FileItem.File -> fileItem.archiveFile
//                    is FileItem.PlaceHolder -> return@clickable
//                }
//                actions(
//                    Action.Navigate.Gallery(
//                        archiveId = archiveFile.archiveId,
//                        fileId = archiveFile.id,
//                        thumbnail = archiveFile.url
//                    )
//                )
//            }
    )
}

@Composable
private fun TextFile(
    modifier: Modifier = Modifier,
    fileItem: FileItem,
) {
    Text(
        modifier = modifier,
        text = fileItem.url
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
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }

    }
}