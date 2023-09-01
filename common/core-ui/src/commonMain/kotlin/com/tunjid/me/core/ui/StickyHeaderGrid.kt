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

package com.tunjid.me.core.ui

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset

@Composable
inline fun StickyHeaderList(
    state: LazyListState,
    modifier: Modifier,
    crossinline headerMatcher: @DisallowComposableCalls (LazyListItemInfo) -> Boolean,
    stickyHeader: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    StickyHeaderLayout(
        lazyState = state,
        modifier = modifier,
        viewportStart = { layoutInfo.viewportStartOffset },
        lazyItems = { layoutInfo.visibleItemsInfo },
        lazyItemOffset = { offset },
        lazyItemHeight = { size },
        headerMatcher = headerMatcher,
        stickyHeader = stickyHeader,
        content = content,
    )
}

@Composable
inline fun StickyHeaderGrid(
    state: LazyGridState,
    modifier: Modifier,
    crossinline headerMatcher: @DisallowComposableCalls (LazyGridItemInfo) -> Boolean,
    stickyHeader: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    StickyHeaderLayout(
        lazyState = state,
        modifier = modifier,
        viewportStart = { layoutInfo.viewportStartOffset },
        lazyItems = { layoutInfo.visibleItemsInfo },
        lazyItemOffset = { offset.y },
        lazyItemHeight = { size.height },
        headerMatcher = headerMatcher,
        stickyHeader = stickyHeader,
        content = content,
    )
}

@Composable
inline fun StickyHeaderStaggeredGrid(
    state: LazyStaggeredGridState,
    modifier: Modifier,
    crossinline headerMatcher: @DisallowComposableCalls (LazyStaggeredGridItemInfo) -> Boolean,
    stickyHeader: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    StickyHeaderLayout(
        lazyState = state,
        modifier = modifier,
        viewportStart = { layoutInfo.viewportStartOffset },
        lazyItems = { layoutInfo.visibleItemsInfo },
        lazyItemOffset = { offset.y },
        lazyItemHeight = { size.height },
        headerMatcher = headerMatcher,
        stickyHeader = stickyHeader,
        content = content,
    )
}

@Composable
inline fun <LazyState : ScrollableState, LazyItem> StickyHeaderLayout(
    lazyState: LazyState,
    modifier: Modifier = Modifier,
    crossinline viewportStart: @DisallowComposableCalls LazyState.() -> Int,
    crossinline lazyItems: @DisallowComposableCalls LazyState.() -> List<LazyItem>,
    crossinline lazyItemOffset: @DisallowComposableCalls LazyItem.() -> Int,
    crossinline lazyItemHeight: @DisallowComposableCalls LazyItem.() -> Int,
    crossinline headerMatcher: @DisallowComposableCalls LazyItem.() -> Boolean,
    stickyHeader: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val headerOffset by remember(lazyState) {
        derivedStateOf {
            val startOffset = viewportStart(lazyState)
            val visibleItems = lazyItems(lazyState)
            val firstCompletelyVisibleItem = visibleItems.firstOrNull { lazyItem ->
                lazyItemOffset(lazyItem) >= startOffset
            } ?: return@derivedStateOf 0

            when (headerMatcher(firstCompletelyVisibleItem)) {
                false -> 0
                true -> lazyItemHeight(firstCompletelyVisibleItem)
                    .minus(lazyItemOffset(firstCompletelyVisibleItem))
                    .let { difference -> if (difference < 0) 0 else -difference }
            }
        }
    }

    Box(modifier = modifier) {
        content()
        Box(
            modifier = Modifier.offset { IntOffset(x = 0, y = headerOffset) }
        ) {
            stickyHeader()
        }
    }
}
