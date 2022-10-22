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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveQuery
import com.tunjid.me.core.ui.StickyHeaderGrid
import com.tunjid.me.feature.LocalRouteServiceLocator
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.mutator.coroutines.asNoOpStateFlowMutator
import com.tunjid.treenav.push
import com.tunjid.treenav.swap
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.serialization.Serializable
import kotlin.math.min

@Serializable
data class ArchiveListRoute(
    override val id: String,
    val kind: ArchiveKind
) : AppRoute {
    @Composable
    override fun Render() {
        ArchiveScreen(
            mutator = LocalRouteServiceLocator.current.locate(this),
        )
    }
}

@Composable
private fun ArchiveScreen(
    mutator: ArchiveListMutator,
) {
    val state by mutator.state.collectAsState()

    if (!state.isInNavRail) GlobalUi(
        state = state,
        onNavigate = mutator.accept
    )

    val gridState = rememberLazyGridState()
    val cardWidth = 350.dp
    val cardWidthPx = with(LocalDensity.current) { cardWidth.toPx() }.toInt()
    val stickyHeaderItem = state.stickyHeader

    Column(
        modifier = Modifier.onGloballyPositioned {
            val gridSize = it.size.width / cardWidthPx
            mutator.accept(Action.GridSize(gridSize))
        }
    ) {
        ArchiveFilters(
            item = state.queryState,
            onChanged = mutator.accept
        )
        StickyHeaderGrid(
            modifier = Modifier.zIndex(-1f),
            lazyState = gridState,
            headerMatcher = { it.key.isHeaderKey },
            stickyHeader = {
                if (stickyHeaderItem != null) StickyHeader(item = stickyHeaderItem)
            }
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(cardWidth),
                content = {
                    items(
                        items = state.items,
                        key = { it.key },
                        span = { item ->
                            mutator.accept(Action.GridSize(maxLineSpan))
                            when (item) {
                                is ArchiveItem.Result -> GridItemSpan(1)
                                is ArchiveItem.Header,
                                is ArchiveItem.Loading -> GridItemSpan(maxLineSpan)
                            }
                        },
                        itemContent = { item ->
                            GridCell(
                                item = item,
                                onAction = mutator.accept,
                                navigate = { path ->
                                    mutator.accept(Action.Navigate {
                                        if (state.isInNavRail) mainNav.swap(route = path.toRoute)
                                        else mainNav.push(route = path.toRoute)
                                    })
                                }
                            )
                        }
                    )
                }
            )
        }
    }

    // Initial load
    LaunchedEffect(state.queryState.startQuery) {
        mutator.accept(
            Action.Fetch.LoadMore(
                query = state.queryState.currentQuery,
                gridSize = state.queryState.gridSize,
            )
        )
    }

    EndlessScroll(
        gridState = gridState,
        gridSize = state.queryState.gridSize,
        currentQuery = state.queryState.currentQuery,
        onAction = mutator.accept
    )

    // Keep list in sync between navbar and fullscreen views
    ListSync(state, gridState)
}

@Composable
private fun GridCell(
    item: ArchiveItem,
    onAction: (Action) -> Unit,
    navigate: (String) -> Unit
) {
    when (item) {
        is ArchiveItem.Header -> StickyHeader(
            item = item
        )

        is ArchiveItem.Loading -> ProgressBar(
            isCircular = item.isCircular
        )

        is ArchiveItem.Result -> ArchiveCard(
            archiveItem = item,
            onAction = onAction,
            onArchiveSelected = { archive ->
                navigate("archives/${archive.kind.type}/${archive.id.value}")
            }
        )
    }
}

@Composable
private fun EndlessScroll(
    gridSize: Int,
    gridState: LazyGridState,
    currentQuery: ArchiveQuery,
    onAction: (Action) -> Unit
) {
    // Endless scrolling
    LaunchedEffect(gridSize, gridState, currentQuery) {
        snapshotFlow {
            val visibleItems = gridState.layoutInfo.visibleItemsInfo
            visibleItems.getOrNull(visibleItems.size / 2)?.key
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { middleItemKey ->
                onAction(Action.ToggleFilter(isExpanded = false))
                middleItemKey.queryOffsetFromKey?.let { queryOffset ->
                    onAction(
                        Action.Fetch.LoadMore(
                            query = currentQuery.copy(offset = queryOffset),
                            gridSize = gridSize,
                        )
                    )
                }
            }
    }
    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.key
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { onAction(Action.LastVisibleKey(it)) }
    }
}

@Composable
private fun ListSync(
    state: State,
    gridState: LazyGridState
) {
    LaunchedEffect(true) {
        val key = state.lastVisibleKey ?: return@LaunchedEffect
        // Item is on screen do nothing
        if (gridState.layoutInfo.visibleItemsInfo.any { it.key == key }) return@LaunchedEffect

        val indexOfKey = state.items.indexOfFirst { it.key == key }
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
                startQuery = ArchiveQuery(kind = ArchiveKind.Articles),
                currentQuery = ArchiveQuery(kind = ArchiveKind.Articles),
            ),
            items = listOf(
                ArchiveItem.Loading(
                    isCircular = true,
                    query = ArchiveQuery(kind = ArchiveKind.Articles)
                )
            )
        ).asNoOpStateFlowMutator()
    )
}
