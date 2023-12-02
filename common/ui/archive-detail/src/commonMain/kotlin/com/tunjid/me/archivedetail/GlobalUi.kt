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
import com.tunjid.me.core.ui.icons.Preview
import com.tunjid.me.scaffold.globalui.InsetFlags
import com.tunjid.me.scaffold.globalui.NavVisibility
import com.tunjid.me.scaffold.globalui.ScreenUiState
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.currentUiState
import com.tunjid.me.scaffold.globalui.rememberFunction
import com.tunjid.me.scaffold.globalui.slices.ToolbarItem
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.routeString

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
            toolbarMenuClickListener = rememberFunction {
                when (it.id) {
                    "gallery" -> if (state.archive != null) actions(Action.Navigate {
                        navState.push(
                            routeString(
                                path = "archives/${state.kind.type}/${state.archive.id.value}/files",
                                queryParams = emptyMap()
                            ).toRoute
                        )
                    })
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
                if (archiveId != null) actions(Action.Navigate {
                    navState.push(
                        routeString(
                            path = "archives/${state.kind.type}/${archiveId.value}/edit",
                            queryParams = mapOf(
                                "thumbnail" to listOfNotNull(
                                    state.archive.thumbnail
                                )
                            )
                        ).toRoute
                    )
                })
            },
            insetFlags = InsetFlags.NO_BOTTOM,
            statusBarColor = MaterialTheme.colorScheme.surface.toArgb(),
        )
    )
}