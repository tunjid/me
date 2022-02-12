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

package com.tunjid.me.common.ui.archivelist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.GridItemSpan
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyGridState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.tunjid.me.common.app.LocalAppDependencies
import com.tunjid.me.common.data.model.ArchiveKind.Articles
import com.tunjid.me.common.data.model.ArchiveQuery
import com.tunjid.me.common.globalui.NavVisibility
import com.tunjid.me.common.globalui.ToolbarItem
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.nav.AppRoute
import com.tunjid.me.common.ui.archiveedit.ArchiveEditRoute
import com.tunjid.me.common.ui.signin.SignInRoute
import com.tunjid.me.common.ui.utilities.InitialUiState
import com.tunjid.mutator.accept
import com.tunjid.mutator.coroutines.asNoOpStateFlowMutator
import com.tunjid.treenav.push
import com.tunjid.treenav.swap
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.serialization.Serializable
import kotlin.math.min

@Serializable
data class ArchiveListRoute(val query: ArchiveQuery) : AppRoute<ArchiveListMutator> {
    override val id: String
        get() = "archive-route-${query.kind}"

    @Composable
    override fun Render() {
        ArchiveScreen(
            mutator = LocalAppDependencies.current.routeDependencies(this),
        )
    }
}

private const val SignIn = "sign-in"

@Composable
private fun ArchiveScreen(
    mutator: ArchiveListMutator,
) {
    val navMutator = LocalAppDependencies.current.appMutator.navMutator
    val state by mutator.state.collectAsState()
    val isInNavRail = state.isInNavRail
    val query = state.queryState.startQuery
    val isSignedIn = state.isSignedIn
    if (!isInNavRail) InitialUiState(
        UiState(
            toolbarShows = true,
            toolbarTitle = query.kind.name,
            toolbarItems = listOfNotNull(
                ToolbarItem(id = SignIn, text = "Sign In")
                    .takeIf { !isSignedIn }
            ),
            toolbarMenuClickListener = { item ->
                when (item.id) {
                    SignIn -> navMutator.accept { push(SignInRoute) }
                }
            },
            fabShows = isSignedIn,
            fabExtended = true,
            fabText = "Create",
            fabIcon = Icons.Default.Add,
            fabClickListener = {
                navMutator.accept {
                    push(
                        ArchiveEditRoute(
                            kind = state.queryState.currentQuery.kind,
                            archiveId = null
                        )
                    )
                }
            },
            navVisibility = NavVisibility.Visible,
            statusBarColor = MaterialTheme.colors.primary.toArgb(),
        )
    )

    val filter = state.queryState
    val items = state.items
    val gridState = rememberLazyGridState()

    Column {
        ArchiveFilters(
            item = filter,
            onChanged = mutator.accept
        )
        LazyVerticalGrid(
            state = gridState,
            cells = GridCells.Adaptive(350.dp),
            content = {
                items(
                    items = items,
                    key = { it.key },
                    span = { item ->
                        mutator.accept(Action.GridSize(maxCurrentLineSpan))
                        when (item) {
                            is ArchiveItem.Result -> GridItemSpan(1)
                            is ArchiveItem.Loading -> GridItemSpan(maxCurrentLineSpan)
                        }
                    },
                    itemContent = { item ->
                        when (item) {
                            is ArchiveItem.Loading -> ProgressBar(isCircular = item.isCircular)
                            is ArchiveItem.Result -> ArchiveCard(
                                archiveItem = item,
                                onAction = mutator.accept,
                                onRouteSelected = { route ->
                                    navMutator.accept {
                                        if (isInNavRail) swap(route = route)
                                        else push(route = route)
                                    }
                                }
                            )
                        }
                    }
                )
            }
        )
    }

    // Initial load
    LaunchedEffect(query) {
        mutator.accept(Action.Fetch.LoadMore(query = state.queryState.currentQuery))
    }

    // Endless scrolling
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.key }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { firstVisibleKey ->
                mutator.accept(Action.ToggleFilter(isExpanded = false))
                mutator.accept(Action.LastVisibleKey(firstVisibleKey))
                firstVisibleKey.queryFromKey?.let { query ->
                    mutator.accept(
                        Action.Fetch.LoadMore(query = query)
                    )
                }
            }
    }

    // Keep list in sync between navbar and destination pages
    LaunchedEffect(true) {
        val key = state.lastVisibleKey ?: return@LaunchedEffect
        // Item is on screen do nothing
        if (gridState.layoutInfo.visibleItemsInfo.any { it.key == key }) return@LaunchedEffect

        val indexOfKey = items.indexOfFirst { it.key == key }
        if (indexOfKey < 0) return@LaunchedEffect

        gridState.scrollToItem(
            index = min(indexOfKey + 1, gridState.layoutInfo.totalItemsCount - 1),
            scrollOffset = 400
        )
    }
}

//@Preview
@Composable
private fun PreviewLoadingState() {
    ArchiveScreen(
        mutator = State(
            queryState = QueryState(
                startQuery = ArchiveQuery(kind = Articles),
                currentQuery = ArchiveQuery(kind = Articles),
            ),
            items = listOf(
                ArchiveItem.Loading(
                    isCircular = true,
                    query = ArchiveQuery(kind = Articles)
                )
            )
        ).asNoOpStateFlowMutator()
    )
}
