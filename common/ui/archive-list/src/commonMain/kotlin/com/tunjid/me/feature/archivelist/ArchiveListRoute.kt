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
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveQuery
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.model.minus
import com.tunjid.me.core.model.plus
import com.tunjid.me.core.ui.StickyHeaderGrid
import com.tunjid.me.feature.LocalScreenStateHolderCache
import com.tunjid.me.scaffold.lifecycle.toActionableState
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.push
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.scan
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class ArchiveListRoute(
    override val id: String,
    val kind: ArchiveKind,
) : AppRoute {
    @Composable
    override fun Render() {
        ArchiveScreen(
            stateHolder = LocalScreenStateHolderCache.current.screenStateHolderFor(this),
        )
    }
}

@Composable
private fun ArchiveScreen(
    stateHolder: ArchiveListStateHolder,
) {
    val screenUiState by stateHolder.toActionableState()
    val (state, actions) = screenUiState

    if (state.isInMainNav) GlobalUi(
        state = state,
        onAction = actions
    )

    val gridState = rememberLazyGridState()
    val cardWidth = 350.dp
    val stickyHeaderItem by remember(state.items) {
        derivedStateOf {
            val firstIndex = gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
            val item = firstIndex?.let(state.items::getOrNull)
            item?.stickyHeader
        }
    }

    Column {
        Spacer(modifier = Modifier.height(16.dp))
        ArchiveFilters(
            item = state.queryState,
            onChanged = actions
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
            ArchiveList(
                gridState = gridState,
                cardWidth = cardWidth,
                items = state.items,
                currentQuery = state.queryState.currentQuery,
                actions = actions
            )
        }
    }

    FilterCollapseEffect(
        gridState = gridState,
        onAction = actions
    )

    gridState.PivotedTilingEffect(
        items = state.items,
        onQueryChanged = {
            actions(Action.Fetch.LoadAround(it ?: state.queryState.currentQuery))
        }
    )
}

@Composable
private fun ArchiveList(
    gridState: LazyGridState,
    items: List<ArchiveItem>,
    currentQuery: ArchiveQuery,
    cardWidth: Dp,
    actions: (Action) -> Unit,
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(cardWidth),
        content = {
            items(
                items = items,
                key = { it.key },
                span = { item ->
                    actions(Action.Fetch.NoColumnsChanged(maxLineSpan))
                    when (item) {
                        is ArchiveItem.Result -> GridItemSpan(1)
                        is ArchiveItem.Header,
                        is ArchiveItem.Loading,
                        -> GridItemSpan(maxLineSpan)
                    }
                },
                itemContent = { item ->
                    GridCell(
                        modifier = Modifier.animateItemPlacement(),
                        item = item,
                        query = currentQuery,
                        onCategoryClicked = { category ->
                            actions(
                                Action.Fetch.QueryChange(
                                    query = currentQuery.copy(offset = 0).let { query ->
                                        if (query.contentFilter.categories.contains(category)) query - category
                                        else query + category
                                    },
                                )
                            )
                        },
                        navigate = { path ->
                            actions(Action.Navigate {
                                mainNav.push(route = path.toRoute)
                            })
                        }
                    )
                }
            )
        }
    )
}

@Composable
private fun GridCell(
    modifier: Modifier = Modifier,
    item: ArchiveItem,
    query: ArchiveQuery,
    onCategoryClicked: (Descriptor.Category) -> Unit,
    navigate: (String) -> Unit,
) {
    when (item) {
        is ArchiveItem.Header -> StickyHeader(
            modifier = modifier,
            item = item
        )

        is ArchiveItem.Loading -> ProgressBar(
            modifier = modifier,
            isCircular = item.isCircular
        )

        is ArchiveItem.Result -> ArchiveCard(
            modifier = modifier,
            query = query,
            archiveItem = item,
            onArchiveSelected = { archive ->
                navigate("archives/${archive.kind.type}/${archive.id.value}")
            },
            onCategoryClicked = onCategoryClicked
        )
    }
}

@Composable
private fun FilterCollapseEffect(
    gridState: LazyGridState,
    onAction: (Action) -> Unit,
) {
    // Close filters when scrolling
    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.firstOrNull()
        }
            .scan<LazyGridItemInfo?, Pair<LazyGridItemInfo, LazyGridItemInfo>?>(null) { oldAndNewInfo, newInfo ->
                when {
                    newInfo == null -> null
                    oldAndNewInfo == null -> newInfo to newInfo
                    else -> oldAndNewInfo.copy(first = oldAndNewInfo.second, second = newInfo)
                }
            }
            .filterNotNull()
            .collect { (oldInfo, newInfo) ->
                val dy = when (oldInfo.index) {
                    newInfo.index -> abs(oldInfo.offset.y - newInfo.offset.y)
                    else -> null
                }
                if (dy != null && dy > 6) onAction(Action.ToggleFilter(isExpanded = false))
            }
    }
}

//@Preview
//@Composable
//private fun PreviewLoadingState() {
//    ArchiveScreen(
//        stateHolder = State(
//            queryState = QueryState(
//                currentQuery = ArchiveQuery(kind = ArchiveKind.Articles),
//            ),
//            items = tiledListOf(
//                ArchiveQuery(kind = ArchiveKind.Articles) to
//                    ArchiveItem.Loading(isCircular = true)
//            )
//        ).asNoOpStateFlowMutator()
//    )
//}
