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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@Composable
inline fun LazyListState.rememberScrollbarState(
    vararg keys: Any? = emptyArray(),
    itemsAvailable: Int,
    crossinline itemIndex: (LazyListItemInfo) -> Int?,
): ScrollbarState =
    rememberScrollbarState(
        itemsAvailable = itemsAvailable,
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
    itemsAvailable: Int,
    crossinline itemIndex: (LazyGridItemInfo) -> Int?,
): ScrollbarState =
    rememberScrollbarState(
        itemsAvailable = itemsAvailable,
        keys = keys,
        items = { layoutInfo.visibleItemsInfo },
        viewportSize = {
            when (layoutInfo.orientation) {
                Orientation.Vertical -> layoutInfo.viewportSize.height
                Orientation.Horizontal -> layoutInfo.viewportSize.width
            }
        },
        itemSize = { it.size.dimension(orientation = layoutInfo.orientation) },
        itemIndex = itemIndex
    )

@Composable
inline fun LazyStaggeredGridState.rememberScrollbarState(
    vararg keys: Any? = emptyArray(),
    itemsAvailable: Int,
    crossinline itemIndex: (LazyStaggeredGridItemInfo) -> Int?,
): ScrollbarState =
    rememberScrollbarState(
        itemsAvailable = itemsAvailable,
        keys = keys,
        items = { layoutInfo.visibleItemsInfo },
        viewportSize = {
            when (layoutInfo.orientation) {
                Orientation.Vertical -> layoutInfo.viewportSize.height
                Orientation.Horizontal -> layoutInfo.viewportSize.width
            }
        },
        itemSize = { it.size.dimension(orientation = layoutInfo.orientation) },
        itemIndex = itemIndex
    )

@Composable
inline fun <LazyState : Any, LazyStateItem> LazyState.rememberScrollbarState(
    itemsAvailable: Int,
    keys: Array<out Any?>,
    crossinline items: LazyState.() -> List<LazyStateItem>,
    crossinline viewportSize: LazyState.() -> Int,
    crossinline itemSize: LazyState.(LazyStateItem) -> Int,
    crossinline itemIndex: (LazyStateItem) -> Int?,
): ScrollbarState {
    var state by remember { mutableStateOf(ScrollbarState.FULL) }
    LaunchedEffect(keys = (arrayOf(this, itemsAvailable) + keys)) {
        snapshotFlow {
            val visibleItemsInfo = items(this@rememberScrollbarState)
            if (visibleItemsInfo.isEmpty()) return@snapshotFlow null

            val firstVisibleItem = visibleItemsInfo.firstOrNull()
                ?: return@snapshotFlow null

            val firstVisibleIndex = itemIndex(firstVisibleItem)
                ?.takeIf { it >= 0 }
                ?: return@snapshotFlow null

            val maxSize = visibleItemsInfo.maxOfOrNull { item ->
                itemSize(this@rememberScrollbarState, item)
            }
                ?.takeIf { it > 0 }
                ?: return@snapshotFlow null

            val itemsVisible = viewportSize(this@rememberScrollbarState) / maxSize

            if (itemsAvailable != 0) ScrollbarState(
                thumbSizePercent = itemsVisible.toFloat() / itemsAvailable,
                thumbTravelPercent = firstVisibleIndex.toFloat() / itemsAvailable
            )
            else ScrollbarState.FULL
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { state = it }
    }
    return state
}
