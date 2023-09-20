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

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveQuery
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.ui.StickyHeaderStaggeredGrid
import com.tunjid.me.core.ui.scrollbar.FastScrollbar
import com.tunjid.me.core.ui.scrollbar.scrollbarState
import com.tunjid.me.feature.LocalScreenStateHolderCache
import com.tunjid.me.scaffold.lifecycle.component1
import com.tunjid.me.scaffold.lifecycle.component2
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.push
import com.tunjid.treenav.swap
import kotlinx.coroutines.flow.*
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
    val (state, actions) = stateHolder
    val updatedItems by rememberUpdatedState(state.items)

    if (state.isInMainNav) GlobalUi(
        state = state,
        onAction = actions
    )

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

    val cardWidth = 340.dp
    val stickyHeaderItem by produceState<ArchiveItem.Header?>(
        initialValue = null,
        key1 = state.isLoading
    ) {
        visibleItemsFlow
            .map { itemInfoList -> itemInfoList.firstOrNull()?.index }
            .distinctUntilChanged()
            .collect { firstIndex ->
                val item = firstIndex?.let(updatedItems::getOrNull)
                value = item?.stickyHeader
            }
    }

    Column {
        Spacer(modifier = Modifier.height(16.dp))
        ArchiveFilters(
            queryState = state.queryState,
            onChanged = actions
        )
        Spacer(modifier = Modifier.height(16.dp))
        StickyHeaderStaggeredGrid(
            modifier = Modifier,
            state = gridState,
            headerMatcher = { it.key.isHeaderKey },
            stickyHeader = {
                    stickyHeaderItem?.let { item ->
                        StickyHeader(
                            modifier = Modifier.fillMaxWidth(),
                            item = item
                        )
                    }
            }
        ) {
            Box {
                ArchiveList(
                    gridState = gridState,
                    cardWidth = cardWidth,
                    items = state.items,
                    isInMainNav = state.isInMainNav,
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
                    onThumbMoved = gridState.scrollbarThumbPositionFunction(
                        state = state,
                        actions = actions
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
    gridState: LazyStaggeredGridState,
    items: List<ArchiveItem>,
    currentQuery: ArchiveQuery,
    isInMainNav: Boolean,
    cardWidth: Dp,
    actions: (Action) -> Unit,
) {
    LazyVerticalStaggeredGrid(
        state = gridState,
        columns = StaggeredGridCells.Adaptive(cardWidth),
        verticalItemSpacing = 16.dp,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
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
                            .animateContentSize(
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    visibilityThreshold = IntSize.VisibilityThreshold
                                )
                            )
                            .animateItemPlacement(
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    visibilityThreshold = IntOffset.VisibilityThreshold
                                )
                            ),
                        item = item,
                        query = currentQuery,
                        onCategoryClicked = { category ->
                            actions(
                                Action.Fetch.QueryChange.ToggleCategory(category)
                            )
                        },
                        navigate = { path ->
                            actions(Action.Navigate {
                                if (isInMainNav) mainNav.push(route = path.toRoute)
                                else mainNav.swap(route = path.toRoute)
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

        is ArchiveItem.Card.Loaded -> ArchiveCard(
            modifier = modifier,
            query = query,
            archive = item.archive,
            onArchiveSelected = { archive ->
                navigate("archives/${archive.kind.type}/${archive.id.value}")
            },
            onCategoryClicked = onCategoryClicked
        )

        is ArchiveItem.Card.PlaceHolder -> ArchiveCard(
            modifier = modifier,
            query = query,
            archive = item.archive,
            onArchiveSelected = { },
            onCategoryClicked = { },
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

@Composable
private fun LazyStaggeredGridState.scrollbarThumbPositionFunction(
    state: State,
    actions: (Action) -> Unit,
): (Float) -> Unit {

    var percentage by remember { mutableStateOf<Float?>(null) }
    val updatedState by rememberUpdatedState(state)

    // Trigger the load to fetch the data required
    LaunchedEffect(percentage) {
        val currentPercentage = percentage ?: return@LaunchedEffect
        val indexToFind = (state.queryState.count * currentPercentage).toInt()
        actions(
            Action.Fetch.LoadAround(
                state.queryState.currentQuery.copy(
                    offset = indexToFind
                )
            )
        )

        // Fast path
        val fastIndex = updatedState.items.indexOfFirst { it.index == indexToFind }
            .takeIf { it > -1 }
        if (fastIndex != null) return@LaunchedEffect scrollToItem(fastIndex)

        // Slow path
        scrollToItem(
            snapshotFlow { updatedState.items.indexOfFirst { it.index == indexToFind } }
                .first { it > -1 }
        )

    }
    return remember {
        { percentage = it }
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
