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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveQuery
import com.tunjid.me.core.ui.StickyHeaderGrid
import com.tunjid.me.feature.LocalScreenStateHolderCache
import com.tunjid.me.scaffold.lifecycle.collectAsStateWithLifecycle
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.mutator.coroutines.asNoOpStateFlowMutator
import com.tunjid.treenav.push
import com.tunjid.treenav.swap
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
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
            mutator = LocalScreenStateHolderCache.current.screenStateHolderFor(this),
        )
    }
}

@Composable
private fun ArchiveScreen(
    mutator: ArchiveListStateHolder,
) {
    val state by mutator.state.collectAsStateWithLifecycle()

    if (!state.isInNavRail) GlobalUi(
        state = state,
        onNavigate = mutator.accept
    )

    val gridState = rememberLazyGridState()
    val cardWidth = 350.dp
    val cardWidthPx = with(LocalDensity.current) { cardWidth.toPx() }.toInt()
//    val stickyHeaderItem = state.stickyHeader
    val stickyHeaderItem by remember(state.items) {
        derivedStateOf {
            val firstIndex = gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
            val item = firstIndex?.let(state.items::getOrNull)
            item?.stickyHeader
        }
    }

    Column(
        modifier = Modifier.onGloballyPositioned {
            val gridSize = it.size.width / cardWidthPx
            mutator.accept(Action.GridSize(gridSize))
        }
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        ArchiveFilters(
            item = state.queryState,
            onChanged = mutator.accept
        )
        Spacer(modifier = Modifier.height(16.dp))
        StickyHeaderGrid(
            modifier = Modifier.zIndex(-1f),
            lazyState = gridState,
            headerMatcher = { it.key.isHeaderKey },
            stickyHeader = {
                stickyHeaderItem?.let { StickyHeader(item = it) }
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
    LaunchedEffect(true) {
        mutator.accept(
            Action.Fetch.LoadMore(
                query = state.queryState.startQuery,
                gridSize = state.queryState.gridSize,
            )
        )
    }

    EndlessScroll(
        gridState = gridState,
        gridSize = state.queryState.gridSize,
        items = state.items,
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
    items: List<ArchiveItem>,
    gridState: LazyGridState,
    onAction: (Action) -> Unit
) {
    // Endless scrolling
    LaunchedEffect(gridSize, items, gridState) {
        snapshotFlow {
            val visibleItems = gridState.layoutInfo.visibleItemsInfo
            val middleItemInfo = visibleItems.getOrNull(visibleItems.size / 2)
            middleItemInfo?.index?.let(items::getOrNull)?.query
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { query ->
                onAction(Action.ToggleFilter(isExpanded = false))
                onAction(
                    Action.Fetch.LoadMore(
                        query = query,
                        gridSize = gridSize,
                    )
                )
            }
    }
    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.key
        }
            .filterIsInstance<String>()
            .distinctUntilChanged()
            .map(Action::LastVisibleKey)
            .collect(onAction)
    }
}

@Composable
private fun ListSync(
    state: State,
    gridState: LazyGridState
) {
    var hasRun by remember { mutableStateOf(false) }
    LaunchedEffect(state.items) {
        if (hasRun || state.items.isEmpty()) return@LaunchedEffect

        val key = state.lastVisibleKey ?: return@LaunchedEffect
        // Item is on screen do nothing
        if (gridState.layoutInfo.visibleItemsInfo.any { it.key == key }) {
            hasRun = true
            return@LaunchedEffect
        }

        val indexOfKey = state.items.indexOfFirst { it.key == key }
        if (indexOfKey < 0) return@LaunchedEffect

        gridState.scrollToItem(
            index = min(indexOfKey + 1, gridState.layoutInfo.totalItemsCount - 1),
            scrollOffset = 400
        )
        hasRun = true
    }
}

//@Preview
@Composable
private fun PreviewLoadingState() {
    ArchiveScreen(
        mutator = State(
            queryState = QueryState(
                startQuery = ArchiveQuery(kind = ArchiveKind.Articles),
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
