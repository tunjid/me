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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.material.MaterialRichText
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveUpsert
import com.tunjid.me.core.ui.RemoteImagePainter
import com.tunjid.me.feature.LocalRouteServiceLocator
import com.tunjid.me.scaffold.nav.AppRoute
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
            .verticalScroll(state = scrollState),
    ) {
        Spacer(modifier = Modifier.padding(8.dp))
        Thumbnail(state.thumbnail)

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
private fun Thumbnail(thumbnail: String?) {
    val painter = RemoteImagePainter(thumbnail)

    if (painter != null) Image(
        painter = painter,
        contentScale = ContentScale.Crop,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp)
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