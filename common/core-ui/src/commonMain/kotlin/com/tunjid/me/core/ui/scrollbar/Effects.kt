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
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun LazyListState.scrollbarState(
    vararg keys: Any? = emptyArray(),
    size: Int,
    indexForItem: (LazyListItemInfo) -> Int?,
): ScrollbarState =
    scrollbarState(
        size = size,
        keys = keys,
        items = { layoutInfo.visibleItemsInfo },
        viewportArea = {
            when (layoutInfo.orientation) {
                Orientation.Vertical -> layoutInfo.viewportSize.height
                Orientation.Horizontal -> layoutInfo.viewportSize.width
            }
        },
        areaForItem = { it.size },
        indexForItem = indexForItem
    )

@Composable
fun LazyGridState.scrollbarState(
    vararg keys: Any? = emptyArray(),
    size: Int,
    indexForItem: (LazyGridItemInfo) -> Int?,
): ScrollbarState =
    scrollbarState(
        size = size,
        keys = keys,
        items = { layoutInfo.visibleItemsInfo },
        viewportArea = { layoutInfo.viewportSize.area },
        areaForItem = { it.size.area },
        indexForItem = indexForItem
    )

@Composable
fun LazyStaggeredGridState.scrollbarState(
    vararg keys: Any? = emptyArray(),
    size: Int,
    indexForItem: (LazyStaggeredGridItemInfo) -> Int?,
): ScrollbarState =
    scrollbarState(
        size = size,
        keys = keys,
        items = { layoutInfo.visibleItemsInfo },
        viewportArea = { layoutInfo.viewportSize.area },
        areaForItem = { it.size.area },
        indexForItem = indexForItem
    )

@Composable
private fun <LazyState : Any, LazyStateItem> LazyState.scrollbarState(
    size: Int,
    keys: Array<out Any?>,
    items: LazyState.() -> List<LazyStateItem>,
    viewportArea: LazyState.() -> Int,
    areaForItem: (LazyStateItem) -> Int,
    indexForItem: (LazyStateItem) -> Int?,
): ScrollbarState {
    var scrollbarState by remember { mutableStateOf(ScrollbarState.FULL) }
    LaunchedEffect(
        keys = (arrayOf(this, size) + keys)
    ) {
        snapshotFlow {
            val visibleItemsInfo = items(this@scrollbarState)
            val minItem = visibleItemsInfo.maxByOrNull(areaForItem)
                ?: return@snapshotFlow null
            val minArea = areaForItem(minItem)
                .takeIf { it > 0 }
                ?: return@snapshotFlow null

            val visible = viewportArea(this@scrollbarState) / minArea
            val info = visibleItemsInfo.firstOrNull()
            val index = info?.let(indexForItem)
                ?.takeIf { it >= 0 }
                ?: return@snapshotFlow null

            scrollbarState(
                available = size,
                visible = visible,
                index = index
            )
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { scrollbarState = it }
    }

    return scrollbarState
}

private val IntSize.area get() = height * width