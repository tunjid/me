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

package com.tunjid.me.feature.archivelist

import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import com.tunjid.me.scaffold.globalui.NavVisibility
import com.tunjid.me.scaffold.globalui.ScreenUiState
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.currentUiState
import com.tunjid.me.scaffold.globalui.rememberFunction
import com.tunjid.me.scaffold.globalui.slices.ToolbarItem
import com.tunjid.treenav.push

private const val SignIn = "sign-in"

@Composable
fun GlobalUi(
    state: State,
    onNavigate: (Action.Navigate) -> Unit
) {
    val query = state.queryState.startQuery
    val isSignedIn = state.isSignedIn
    ScreenUiState(
        UiState(
            toolbarShows = true,
            toolbarTitle = query.kind.name,
            toolbarItems = listOfNotNull(
                ToolbarItem(
                    id = SignIn,
                    text = "Sign In"
                ).takeIf { !isSignedIn }
            ),
            toolbarMenuClickListener = rememberFunction { item ->
                when (item.id) {
                    SignIn -> onNavigate(Action.Navigate {
                        currentNav.push("sign-in".toRoute)
                    })
                }
            },
            fabShows = if (state.hasFetchedAuthStatus) isSignedIn else currentUiState.fabShows,
            fabExtended = true,
            fabText = "Create",
            fabIcon = Icons.Default.Add,
            fabClickListener = rememberFunction {
                onNavigate(Action.Navigate {
                    val kind = state.queryState.currentQuery.kind
                    currentNav.push("archives/${kind.type}/create".toRoute)
                })
            },
            navVisibility = NavVisibility.Visible,
            statusBarColor = MaterialTheme.colors.primary.toArgb(),
        )
    )
}