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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import com.tunjid.me.core.ui.icons.Preview
import com.tunjid.me.scaffold.globalui.*
import com.tunjid.me.scaffold.globalui.slices.ToolbarItem

@Composable
internal fun GlobalUi(
    state: State,
    onAction: (Action) -> Unit
) {
    ScreenUiState(
        UiState(
            toolbarShows = true,
            toolbarTitle = "${if (state.upsert.id == null) "Create" else "Edit"} ${state.kind.name}",
            toolbarItems = listOfNotNull(
                ToolbarItem(
                    id = "preview",
                    text = "Preview",
                    imageVector = Icons.Default.Preview
                ).takeIf { state.isEditing },
                ToolbarItem(
                    id = "edit",
                    text = "Edit",
                    imageVector = Icons.Default.Edit
                ).takeIf { !state.isEditing }
            ),
            toolbarMenuClickListener = rememberFunction {
                onAction(Action.ToggleEditView)
            },
            fabShows = true,
            fabText = if (state.upsert.id == null) "Create" else "Save",
            fabIcon = Icons.Default.Done,
            fabExtended = true,
            fabEnabled = !state.isSubmitting,
            fabClickListener = rememberFunction(state.kind, state.upsert, state.toUpload) {
                onAction(
                    Action.Load.Submit(
                        kind = state.kind,
                        upsert = state.upsert,
                        headerPhoto = state.toUpload,
                    )
                )
            },
            snackbarMessages = state.messages,
            snackbarMessageConsumer = rememberFunction {
                onAction(Action.MessageConsumed(it))
            },
            navVisibility = NavVisibility.GoneIfBottomNav,
            insetFlags = InsetFlags.NO_BOTTOM,
            statusBarColor = MaterialTheme.colorScheme.primary.toArgb(),
        )
    )
}