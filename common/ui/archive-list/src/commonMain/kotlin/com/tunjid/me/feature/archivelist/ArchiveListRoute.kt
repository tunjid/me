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

// See YouTrack: KTIJ-18375
@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package com.tunjid.me.feature.archivelist

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tunjid.composables.scrollbars.scrollable.rememberScrollbarThumbMover
import com.tunjid.composables.scrollbars.scrollable.staggeredgrid.scrollbarState
import com.tunjid.composables.stickyheader.staggeredgrid.StickyHeaderStaggeredGrid
import com.tunjid.me.core.model.ArchiveQuery
import com.tunjid.me.core.ui.scrollbar.FastScrollbar
import com.tunjid.me.scaffold.adaptive.routeOf
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.strings.RouteParams
import kotlinx.coroutines.flow.*
import kotlin.math.abs

fun ArchiveListRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams
)

@Composable
internal fun ArchiveListScreen(
    movableSharedElementScope: MovableSharedElementScope,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val updatedItems by rememberUpdatedState(state.items)
    val gridState = state.listState ?: LazyStaggeredGridState()
    val scope = rememberCoroutineScope()
    val visibleItemsFlow = remember(state.isLoading) {
        if (state.isLoading) emptyFlow()
        else snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo
        }
            .stateIn(
                scope = scope,
                initialValue = emptyList(),
                started = SharingStarted.WhileSubscribed()
            )
    }

    val cardWidth = 320.dp
    Column(
        modifier = modifier,
    ) {
        ArchiveFilters(
            queryState = state.queryState,
            onChanged = actions
        )
        Spacer(modifier = Modifier.height(16.dp))
        StickyHeaderStaggeredGrid(
            modifier = Modifier,
            state = gridState,
            isStickyHeaderItem = { it.key.isHeaderKey },
            stickyHeader = { index, _, _ ->
                updatedItems.getOrNull(index)
                    ?.stickyHeader
                    ?.let { item ->
                        StickyHeader(
                            modifier = Modifier.fillMaxWidth(),
                            item = item
                        )
                    }
            }
        ) {
            Box {
                ArchiveList(
                    movableSharedElementScope = movableSharedElementScope,
                    gridState = gridState,
                    cardWidth = cardWidth,
                    items = state.items,
                    isInPrimaryNav = state.isInPrimaryNav,
                    currentQuery = state.queryState.currentQuery,
                    actions = actions
                )

                val scrollbarState = gridState.scrollbarState(
                    itemsAvailable = state.queryState.count.toInt(),
                    itemIndex = { itemInfo ->
                        updatedItems.getOrNull(itemInfo.index)?.index ?: -1
                    }
                )

                FastScrollbar(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .width(12.dp),
                    state = scrollbarState,
                    scrollInProgress = gridState.isScrollInProgress,
                    orientation = Orientation.Vertical,
                    onThumbMoved = rememberScrollbarThumbMover(
                        itemsAvailable = state.queryState.count.toInt(),
                        scroll = scroll@{ index ->
                            actions(
                                Action.Fetch.LoadAround(
                                    query = state.queryState.currentQuery.copy(offset = index)
                                )
                            )

                            // Fast path
                            val fastIndex = updatedItems.indexOfFirst { it.index == index }
                                .takeIf { it > -1 }
                            if (fastIndex != null) return@scroll gridState.scrollToItem(fastIndex)

                            // Slow path
                            gridState.scrollToItem(
                                snapshotFlow { updatedItems.indexOfFirst { it.index == index } }
                                    .first { it > -1 }
                            )
                        }
                    ),
                )
            }
        }
    }

    FilterCollapseEffect(
        infoFlow = visibleItemsFlow,
        onAction = actions
    )

    gridState.PivotedTilingEffect(
        items = state.items,
        indexSelector = kotlin.ranges.IntRange::first,
        onQueryChanged = {
            actions(Action.Fetch.LoadAround(it ?: state.queryState.currentQuery))
        }
    )

    SaveScrollPositionEffect(
        infoFlow = visibleItemsFlow,
        onAction = actions
    )

    GridSizeUpdateEffect(
        infoFlow = visibleItemsFlow,
        onAction = actions
    )
}

@Composable
private fun ArchiveList(
    movableSharedElementScope: MovableSharedElementScope,
    gridState: LazyStaggeredGridState,
    items: List<ArchiveItem>,
    currentQuery: ArchiveQuery,
    isInPrimaryNav: Boolean,
    cardWidth: Dp,
    actions: (Action) -> Unit,
) {
    LazyVerticalStaggeredGrid(
        state = gridState,
        columns = StaggeredGridCells.Adaptive(cardWidth),
        verticalItemSpacing = 16.dp,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 36.dp
        ),
        content = {
            items(
                items = items,
                key = { it.key },
                contentType = { it.contentType },
                span = { item ->
                    when (item) {
                        is ArchiveItem.Card.Loaded,
                        is ArchiveItem.Card.PlaceHolder,
                        -> StaggeredGridItemSpan.SingleLane

                        is ArchiveItem.Header,
                        is ArchiveItem.Loading,
                        -> StaggeredGridItemSpan.FullLine
                    }
                },
                itemContent = { item ->
                    GridCell(
                        modifier = Modifier
                            .animateContentSize(animationSpec = ItemSizeSpec)
                            .animateItem(),
                        movableSharedElementScope = movableSharedElementScope,
                        item = item,
                        query = currentQuery,
                        actions = actions,
                    )
                }
            )
        }
    )
}

@Composable
private fun GridCell(
    movableSharedElementScope: MovableSharedElementScope,
    modifier: Modifier = Modifier,
    item: ArchiveItem,
    query: ArchiveQuery,
    actions: (Action) -> Unit,
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

        is ArchiveItem.Card.Loaded -> ArchiveCard(
            movableSharedElementScope = movableSharedElementScope,
            modifier = modifier,
            query = query,
            archive = item.archive,
            actions = actions,
        )

        is ArchiveItem.Card.PlaceHolder -> ArchiveCard(
            movableSharedElementScope = movableSharedElementScope,
            modifier = modifier,
            query = query,
            archive = item.archive,
            actions = { },
        )
    }
}

@Composable
private fun FilterCollapseEffect(
    infoFlow: Flow<List<LazyStaggeredGridItemInfo>>,
    onAction: (Action) -> Unit,
) {
    // Close filters when scrolling
    LaunchedEffect(infoFlow) {
        infoFlow
            .map { it.firstOrNull() }
            .scan<LazyStaggeredGridItemInfo?, Pair<LazyStaggeredGridItemInfo, LazyStaggeredGridItemInfo>?>(
                null
            ) { oldAndNewInfo, newInfo ->
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


@Composable
private fun SaveScrollPositionEffect(
    infoFlow: Flow<List<LazyStaggeredGridItemInfo>>,
    onAction: (Action) -> Unit,
) {
    // Close filters when scrolling
    LaunchedEffect(infoFlow) {
        infoFlow
            .filter(List<LazyStaggeredGridItemInfo>::isNotEmpty)
            .map { items ->
                Action.ListStateChanged(
                    firstVisibleItemScrollOffset = items.first().offset.y,
                    firstVisibleItemIndex = items.first().index
                )
            }
            .collect(onAction)
    }
}

@Composable
private fun GridSizeUpdateEffect(
    infoFlow: Flow<List<LazyStaggeredGridItemInfo>>,
    onAction: (Action) -> Unit,
) {
    LaunchedEffect(infoFlow) {
        infoFlow
            .filter(List<LazyStaggeredGridItemInfo>::isNotEmpty)
            .map { items ->
                val maxLane = items.maxOf(LazyStaggeredGridItemInfo::lane)
                Action.Fetch.NoColumnsChanged(noColumns = maxLane + 1)
            }
            .distinctUntilChanged()
            .collect(onAction)
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


private val ItemSizeSpec = itemSpring(IntSize.VisibilityThreshold)

private fun <T> itemSpring(
    visibilityThreshold: T
) = spring(
    stiffness = Spring.StiffnessMedium,
    visibilityThreshold = visibilityThreshold
)