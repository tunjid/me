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

package com.tunjid.me.archiveedit

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.material3.Material3RichText
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveUpsert
import com.tunjid.me.core.ui.AsyncRasterImage
import com.tunjid.me.core.ui.dragdrop.dropTarget
import com.tunjid.me.feature.LocalScreenStateHolderCache
import com.tunjid.me.scaffold.lifecycle.toActionableState
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.permissions.Permission
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

private const val BODY_INDEX = 11

@Serializable
data class ArchiveEditRoute(
    override val id: String,
    val kind: ArchiveKind,
    val archiveId: ArchiveId?,
) : AppRoute {
    @Composable
    override fun Render() {
        ArchiveEditScreen(
            stateHolder = LocalScreenStateHolderCache.current.screenStateHolderFor(this),
        )
    }

    override val supportingRoute
        get() = when (archiveId) {
            null -> null
            else -> "archives/${kind.type}/${archiveId.value}/files?type=image&dndEnabled=true"
        }
}

@Composable
private fun ArchiveEditScreen(stateHolder: ArchiveEditStateHolder) {
    val screenUiState by stateHolder.toActionableState()
    val (state, actions) = screenUiState
    val upsert = state.upsert
    val scrollState = rememberLazyListState()

    // Disable body text field from scrolling until all items are offscreen
    var canConsumeScrollEventsInBody by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.layoutInfo }
            .map { it.visibleItemsInfo.firstOrNull()?.key == BODY_INDEX }
            .distinctUntilChanged()
            .collect { canConsumeScrollEventsInBody = it }
    }

    val scope = rememberCoroutineScope()

    GlobalUi(
        state = state,
        onAction = actions
    )

    LazyColumn(
        modifier = Modifier
            .dropTarget(
                onStarted = { _, _ ->
                    val (action, acceptedDrag) = when (state.hasStoragePermissions) {
                        true -> Action.Drag.Window(inside = true) to true
                        false -> Action.RequestPermission(Permission.ReadExternalStorage) to false
                    }
                    actions(action)
                    acceptedDrag
                },
                onEntered = { actions(Action.Drag.Window(inside = true)) },
                onExited = { actions(Action.Drag.Window(inside = false)) },
                onEnded = { actions(Action.Drag.Window(inside = false)) },
                onDropped = { _, _ ->
                    actions(Action.Drag.Window(inside = false))
                    false
                }
            ),
        state = scrollState,
    ) {
        spacer(8.dp)
        dragDropThumbnail(
            thumbnail = state.thumbnail,
            hasStoragePermission = state.hasStoragePermissions,
            dragLocation = state.dragLocation,
            onAction = actions
        )

        spacer(8.dp)
        titleEditor(
            title = upsert.title,
            onEdit = actions
        )

        spacer(8.dp)
        descriptionEditor(
            description = upsert.description,
            onEdit = actions
        )

        spacer(8.dp)
        videoUrlEditor(
            videoUrl = upsert.videoUrl,
            onEdit = actions
        )

        spacer(8.dp)
        chipsEditor(
            upsert = upsert,
            chipsState = state.chipsState,
            onAction = actions
        )

        spacer(16.dp)
        if (state.isEditing) bodyEditor(
            body = state.body,
            canConsumeScrollEvents = canConsumeScrollEventsInBody,
            onScrolled = scrollState::dispatchRawDelta,
            onInteractedWith = {
                scope.launch { scrollState.animateScrollToItem(BODY_INDEX) }
            },
            onEdit = actions
        )
        else bodyPreview(
            body = upsert.body
        )
    }
}

private fun LazyListScope.dragDropThumbnail(
    thumbnail: String?,
    hasStoragePermission: Boolean,
    dragLocation: DragLocation,
    onAction: (Action) -> Unit,
) = item {
    val borderColor by animateColorAsState(
        when (dragLocation) {
            DragLocation.InWindow -> MaterialTheme.colorScheme.errorContainer
            DragLocation.InThumbnail -> MaterialTheme.colorScheme.primaryContainer
            DragLocation.None -> Color.Transparent
        }
    )
    AsyncRasterImage(
        imageUrl = thumbnail,
        modifier = Modifier
            .heightIn(max = 300.dp)
            .aspectRatio(ratio = 16f / 9f)
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium,
            )
            .dropTarget(
                onStarted = { _, _ -> hasStoragePermission },
                onEntered = { onAction(Action.Drag.Thumbnail(inside = true)) },
                onExited = { onAction(Action.Drag.Thumbnail(inside = false)) },
                onDropped = { uris, _ ->
                    onAction(Action.Drag.Thumbnail(inside = false))
                    onAction(Action.Drop(uris = uris))
                    true
                },
            ) { onAction(Action.Drag.Thumbnail(inside = false)) }
    )
}

private fun LazyListScope.titleEditor(
    title: String,
    onEdit: (Action.TextEdit) -> Unit,
) = item {
    TextField(
        value = title,
        maxLines = 2,
        colors = Unstyled(),
        textStyle = LocalTextStyle.current.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 24.sp
        ),
        label = { Text(text = "Title", fontSize = 24.sp) },
        onValueChange = { onEdit(Action.TextEdit.Title(it)) }
    )
}

private fun LazyListScope.descriptionEditor(
    description: String,
    onEdit: (Action.TextEdit) -> Unit,
) = item {
    TextField(
        value = description,
        maxLines = 2,
        colors = Unstyled(),
        textStyle = LocalTextStyle.current.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp
        ),
        label = { Text(text = "Description", fontSize = 18.sp) },
        onValueChange = { onEdit(Action.TextEdit.Description(it)) }
    )
}

private fun LazyListScope.videoUrlEditor(
    videoUrl: String?,
    onEdit: (Action.TextEdit) -> Unit,
) = item {
    TextField(
        value = videoUrl ?: "",
        maxLines = 1,
        colors = Unstyled(),
        textStyle = LocalTextStyle.current.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp
        ),
        label = { Text(text = "Video Url") },
        onValueChange = { onEdit(Action.TextEdit.VideoUrl(it)) }
    )
}

private fun LazyListScope.bodyEditor(
    body: TextFieldValue,
    canConsumeScrollEvents: Boolean,
    onScrolled: (Float) -> Float,
    onInteractedWith: () -> Unit,
    onEdit: (Action.TextEdit) -> Unit,
) = item(key = BODY_INDEX) {
    Box(
        modifier = Modifier
            .fillParentMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        var layoutResult: TextLayoutResult? by remember { mutableStateOf(null) }
        // TODO: Revert to TextField when b/240975569 and b/235383908 are fixed
        BasicTextField(
            modifier = Modifier
                .fillMaxSize()
                .dropTarget(
                    onStarted = { _, _ -> true },
                    onEntered = onInteractedWith,
                    onMoved = { offset ->
                        layoutResult
                            ?.getOffsetForPosition(offset)
                            ?.let { cursorIndex ->
                                onEdit(
                                    Action.TextEdit.Body.CursorIndex(cursorIndex)
                                )
                            }
                    },
                    onDropped = { uris, offset ->
                        if (uris.isNotEmpty()) layoutResult
                            ?.getOffsetForPosition(offset)
                            ?.let { cursorIndex ->
                                onEdit(
                                    Action.TextEdit.Body.ImageDrop(
                                        index = cursorIndex,
                                        uri = uris.first()
                                    )
                                )
                            }
                        true
                    },
                )
                .onFocusChanged { if (it.hasFocus) onInteractedWith() },
            value = body,
//            colors = Unstyled(),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            onTextLayout = { layoutResult = it },
            textStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                lineHeight = 24.sp
            ),
//            label = { Text(text = "Body") },
            onValueChange = { onEdit(Action.TextEdit.Body.Edit(it)) }
        )

        // Overlay a box over the TextField to intercept scroll events
        Spacer(
            modifier = Modifier
                .fillMaxSize(if (canConsumeScrollEvents) 0f else 1f)
                .scrollable(
                    orientation = Orientation.Vertical,
                    reverseDirection = true,
                    // Pass the scroll events up the tree to the parent lazy list state to consume
                    state = rememberScrollableState(onScrolled)
                )
                .pointerInput(Unit) {
                    detectTapGestures {
                        onInteractedWith()
                    }
                }
        )
    }
}

private fun LazyListScope.bodyPreview(body: String) = item(
    key = BODY_INDEX
) {
    Material3RichText(
        modifier = Modifier
            .defaultMinSize(minHeight = 500.dp)
            .padding(horizontal = 16.dp)
    ) {
        Markdown(
            content = body
        )
    }
}

private fun LazyListScope.chipsEditor(
    upsert: ArchiveUpsert,
    chipsState: ChipsState,
    onAction: (Action) -> Unit,
) = item {
    EditChips(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp),
        upsert = upsert,
        state = chipsState,
        onChanged = onAction
    )
}

private fun LazyListScope.spacer(size: Dp) = item {
    Spacer(modifier = Modifier.padding(size))
}

@Composable
private fun Unstyled() = TextFieldDefaults.textFieldColors(
    containerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = MaterialTheme.colorScheme.onSurface,
)
