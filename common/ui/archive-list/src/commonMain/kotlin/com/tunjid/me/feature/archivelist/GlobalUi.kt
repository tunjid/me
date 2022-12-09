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
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import com.tunjid.me.scaffold.globalui.*
import com.tunjid.me.scaffold.globalui.slices.ToolbarItem
import com.tunjid.treenav.push

private const val SORT_ORDER = "SORT_ORDER"
private const val SIGN_IN = "SIGN_IN"

@Composable
fun GlobalUi(
    state: State,
    onAction: (Action) -> Unit
) {
    val queryState = state.queryState
    val query = queryState.currentQuery
    val isSignedIn = state.isSignedIn

    ScreenUiState(
        UiState(
            toolbarShows = true,
            toolbarTitle = query.kind.name,
            toolbarItems = listOfNotNull(
                ToolbarItem(
                    id = SORT_ORDER,
                    text =
                    if (state.queryState.currentQuery.desc) "Ascending"
                    else "Descending",
                    imageVector =
                    if (state.queryState.currentQuery.desc) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown
                ),
                ToolbarItem(
                    id = SIGN_IN,
                    text = "Sign In",
                    imageVector = Icons.Default.AccountBox
                ).takeIf { !isSignedIn }
            ),
            toolbarMenuClickListener = rememberFunction(query) { item ->
                when (item.id) {
                    SORT_ORDER -> onAction(
                        Action.Fetch.Reset(
                            query = query.copy(
                                desc = !query.desc,
                                offset = (queryState.count - query.offset).toInt()
                            )
                        )
                    )

                    SIGN_IN -> onAction(
                        Action.Navigate {
                            mainNav.push("sign-in".toRoute)
                        }
                    )
                }
            },
            fabShows = if (state.hasFetchedAuthStatus) isSignedIn else currentUiState.fabShows,
            fabExtended = true,
            fabText = "Create",
            fabIcon = Icons.Default.Add,
            fabClickListener = rememberFunction {
                onAction(Action.Navigate {
                    val kind = state.queryState.currentQuery.kind
                    val route = "archives/${kind.type}/create".toRoute
                    mainNav.push(route)
                })
            },
            navVisibility = NavVisibility.Visible,
            statusBarColor = MaterialTheme.colors.primary.toArgb(),
        )
    )
}