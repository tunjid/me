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

package com.tunjid.me.core.ui.scrollbar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@Composable
inline fun LazyListState.rememberScrollbarState(
    vararg keys: Any? = emptyArray(),
    size: Int,
    crossinline itemIndex: (LazyListItemInfo) -> Int?,
): ScrollbarState =
    rememberScrollbarState(
        size = size,
        keys = keys,
        items = { layoutInfo.visibleItemsInfo },
        viewportSize = {
            when (layoutInfo.orientation) {
                Orientation.Vertical -> layoutInfo.viewportSize.height
                Orientation.Horizontal -> layoutInfo.viewportSize.width
            }
        },
        itemSize = { it.size },
        itemIndex = itemIndex
    )

@Composable
inline fun LazyGridState.rememberScrollbarState(
    vararg keys: Any? = emptyArray(),
    size: Int,
    crossinline itemIndex: (LazyGridItemInfo) -> Int?,
): ScrollbarState =
    rememberScrollbarState(
        size = size,
        keys = keys,
        items = { layoutInfo.visibleItemsInfo },
        viewportSize = {
            when (layoutInfo.orientation) {
                Orientation.Vertical -> layoutInfo.viewportSize.height
                Orientation.Horizontal -> layoutInfo.viewportSize.width
            }
        },
        itemSize = { it.size.area },
        itemIndex = itemIndex
    )

@Composable
@OptIn(ExperimentalFoundationApi::class)
inline fun LazyStaggeredGridState.rememberScrollbarState(
    vararg keys: Any? = emptyArray(),
    size: Int,
    crossinline itemIndex: (LazyStaggeredGridItemInfo) -> Int?,
): ScrollbarState =
    rememberScrollbarState(
        size = size,
        keys = keys,
        items = { layoutInfo.visibleItemsInfo },
        viewportSize = {
            when (layoutInfo.orientation) {
                Orientation.Vertical -> layoutInfo.viewportSize.height
                Orientation.Horizontal -> layoutInfo.viewportSize.width
            }
        },
        itemSize = { it.size.area },
        itemIndex = itemIndex
    )

@Composable
inline fun <LazyState : Any, LazyStateItem> LazyState.rememberScrollbarState(
    size: Int,
    keys: Array<out Any?>,
    crossinline items: LazyState.() -> List<LazyStateItem>,
    crossinline viewportSize: LazyState.() -> Int,
    crossinline itemSize: (LazyStateItem) -> Int,
    crossinline itemIndex: (LazyStateItem) -> Int?,
): ScrollbarState {
    var state by remember { mutableStateOf(ScrollbarState.FULL) }
    LaunchedEffect(keys = (arrayOf(this, size) + keys)) {
        snapshotFlow {
            val visibleItemsInfo = items(this@rememberScrollbarState)
            if (visibleItemsInfo.isEmpty()) return@snapshotFlow null

            val avgSize = (visibleItemsInfo.sumOf(itemSize) / visibleItemsInfo.size)
                .takeIf { it > 0 }
                ?: return@snapshotFlow null

            val itemsVisible = viewportSize(this@rememberScrollbarState) / avgSize
            val firstVisibleItem = visibleItemsInfo.firstOrNull()
            val firstVisibleIndex = firstVisibleItem?.let(itemIndex)
                ?.takeIf { it >= 0 }
                ?: return@snapshotFlow null

            scrollbarState(
                itemsAvailable = size,
                itemsVisible = itemsVisible,
                firstVisibleIndex = firstVisibleIndex
            )
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { state = it }
    }
    return state
}

val IntSize.area get() = height * width