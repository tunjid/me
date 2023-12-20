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

package com.tunjid.me.archivedetail

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import com.tunjid.me.scaffold.globalui.InsetFlags
import com.tunjid.me.scaffold.globalui.NavVisibility
import com.tunjid.me.scaffold.globalui.ScreenUiState
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.currentUiState
import com.tunjid.me.scaffold.globalui.rememberFunction
import com.tunjid.me.scaffold.globalui.slices.ToolbarItem

@Composable
fun GlobalUi(state: State, actions: (Action) -> Unit) {
    ScreenUiState(
        UiState(
            toolbarShows = true,
            toolbarTitle = state.archive?.title ?: "Detail",
            toolbarItems = listOf(
                ToolbarItem(
                    id = "gallery",
                    text = "Gallery",
                    imageVector = Icons.Default.Email
                )
            ),
            toolbarMenuClickListener = rememberFunction(state.archive?.id) {
                when (it.id) {
                    "gallery" -> if (state.archive != null) actions(
                        Action.Navigate.Files(
                            kind = state.kind,
                            archiveId = state.archive.id,
                            thumbnail = state.archive.thumbnail,
                        )
                    )
                }
            },
            navVisibility = NavVisibility.Visible,
            // Prevents UI from jittering as load starts
            fabShows = if (state.hasFetchedAuthStatus) state.canEdit else currentUiState.fabShows,
            fabExtended = true,
            fabText = "Edit",
            fabIcon = Icons.Default.Edit,
            fabClickListener = rememberFunction(state.archive?.id) {
                val archiveId = state.archive?.id
                if (archiveId != null) actions(
                    Action.Navigate.Edit(
                        kind = state.kind,
                        archiveId = state.archive.id,
                        thumbnail = state.archive.thumbnail,
                    )
                )
            },
            insetFlags = InsetFlags.NO_BOTTOM,
            statusBarColor = MaterialTheme.colorScheme.surface.toArgb(),
        )
    )
}