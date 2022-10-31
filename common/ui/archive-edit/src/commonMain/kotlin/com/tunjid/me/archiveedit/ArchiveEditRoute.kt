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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.material.MaterialRichText
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveUpsert
import com.tunjid.me.core.ui.Thumbnail
import com.tunjid.me.core.ui.dragdrop.dropTarget
import com.tunjid.me.feature.LocalRouteServiceLocator
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.permissions.Permission
import kotlinx.serialization.Serializable

@Serializable
data class ArchiveEditRoute(
    override val id: String,
    val kind: ArchiveKind,
    val archiveId: ArchiveId?
) : AppRoute {
    @Composable
    override fun Render() {
        ArchiveEditScreen(
            mutator = LocalRouteServiceLocator.current.locate(this),
        )
    }
}

@Composable
private fun ArchiveEditScreen(mutator: ArchiveEditMutator) {
    val state by mutator.state.collectAsState()
    val upsert = state.upsert
    val scrollState = rememberScrollState()
    val navBarSizeDp = with(LocalDensity.current) { state.navBarSize.toDp() }

    GlobalUi(
        state = state,
        onAction = mutator.accept
    )
    Column(
        modifier = Modifier
            .verticalScroll(state = scrollState)
            .dropTarget(
                onDragStarted = { _, _ ->
                    val (action, acceptedDrag) = when (state.hasStoragePermissions) {
                        true -> Action.Drag.Window(inside = true) to true
                        false -> Action.RequestPermission(Permission.ReadExternalStorage) to false
                    }
                    mutator.accept(action)
                    acceptedDrag
                },
                onDragEntered = { mutator.accept(Action.Drag.Window(inside = true)) },
                onDragExited = { mutator.accept(Action.Drag.Window(inside = false)) },
                onDragEnded = { mutator.accept(Action.Drag.Window(inside = false)) },
                onDropped = { _, _ ->
                    mutator.accept(Action.Drag.Window(inside = false))
                    false
                }
            ),
    ) {
        Spacer(modifier = Modifier.padding(8.dp))
        DragDropThumbnail(
            thumbnail = state.thumbnail,
            hasStoragePermission = state.hasStoragePermissions,
            dragStatus = state.dragStatus,
            onAction = mutator.accept
        )

        Spacer(modifier = Modifier.padding(8.dp))
        TitleEditor(
            title = upsert.title,
            onEdit = mutator.accept
        )

        Spacer(modifier = Modifier.padding(8.dp))
        DescriptionEditor(
            description = upsert.description,
            onEdit = mutator.accept
        )

        Spacer(modifier = Modifier.padding(8.dp))
        ChipsEditor(
            upsert = upsert,
            chipsState = state.chipsState,
            onAction = mutator.accept
        )

        Spacer(modifier = Modifier.padding(16.dp))
        when (state.isEditing) {
            true -> BodyEditor(
                body = upsert.body,
                onEdit = mutator.accept
            )

            false -> BodyPreview(
                body = upsert.body
            )
        }

        Spacer(modifier = Modifier.padding(64.dp + navBarSizeDp))
    }
}

@Composable
private fun DragDropThumbnail(
    thumbnail: String?,
    hasStoragePermission: Boolean,
    dragStatus: DragStatus,
    onAction: (Action) -> Unit
) {
    // Create a var so edits can be captured
    val permissionState = remember { mutableStateOf(hasStoragePermission) }
    permissionState.value = hasStoragePermission

    val borderColor by animateColorAsState(
        when (dragStatus) {
            DragStatus.InWindow -> Color.Red
            DragStatus.InThumbnail -> Color.Green
            DragStatus.None -> Color.Transparent
        }
    )
    Thumbnail(
        imageUrl = thumbnail,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp)
            .border(
                width = 2.dp,
                color = borderColor
            )
            .dropTarget(
                onDragStarted = { _, _ -> permissionState.value },
                onDragEntered = { onAction(Action.Drag.Thumbnail(inside = true)) },
                onDragExited = { onAction(Action.Drag.Thumbnail(inside = false)) },
                onDropped = { uris, _ ->
                    onAction(Action.Drag.Thumbnail(inside = false))
                    onAction(Action.Drop(uris = uris))
                    true
                },
                onDragEnded = { onAction(Action.Drag.Thumbnail(inside = false)) },
            )
    )
}

@Composable
private fun TitleEditor(
    title: String,
    onEdit: (Action.TextEdit) -> Unit,
) {
    TextField(
        value = title,
        maxLines = 2,
        colors = Unstyled(),
        textStyle = LocalTextStyle.current.copy(
            color = MaterialTheme.colors.onSurface,
            fontSize = 24.sp
        ),
        label = { Text(text = "Title", fontSize = 24.sp) },
        onValueChange = { onEdit(Action.TextEdit.Title(it)) }
    )
}

@Composable
private fun DescriptionEditor(
    description: String,
    onEdit: (Action.TextEdit) -> Unit,
) {
    TextField(
        value = description,
        maxLines = 2,
        colors = Unstyled(),
        textStyle = LocalTextStyle.current.copy(
            color = MaterialTheme.colors.onSurface,
            fontSize = 18.sp
        ),
        label = { Text(text = "Description", fontSize = 18.sp) },
        onValueChange = { onEdit(Action.TextEdit.Description(it)) }
    )
}

@Composable
private fun BodyEditor(
    body: String,
    onEdit: (Action.TextEdit) -> Unit,
) {
    TextField(
        value = body,
        colors = Unstyled(),
        textStyle = LocalTextStyle.current.copy(
            color = MaterialTheme.colors.onSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        label = { Text(text = "Body") },
        onValueChange = { onEdit(Action.TextEdit.Body(it)) }
    )
}

@Composable
private fun BodyPreview(body: String) {
    MaterialRichText(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Markdown(
            content = body
        )
    }
}

@Composable
private fun ChipsEditor(
    upsert: ArchiveUpsert,
    chipsState: ChipsState,
    onAction: (Action) -> Unit,
) {
    EditChips(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp),
        upsert = upsert,
        state = chipsState,
        onChanged = onAction
    )
}

@Composable
private fun Unstyled() = TextFieldDefaults.textFieldColors(
    backgroundColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = MaterialTheme.colors.onSurface,
)