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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

///**
// * Remembers a [ScrollbarState] driven by the changes in a [ScrollbarState].
// */
//@Composable
//fun ScrollState.scrollbarState(): ScrollbarState = ScrollbarState(
//    thumbSizePercent = viewportSize.toFloat() / maxValue,
//    thumbTravelPercent = value.toFloat() / maxValue,
//)

/**
 * Remembers a [ScrollbarState] driven by the changes in a [LazyListState].
 *
 * @param itemsAvailable the total amount of items available to scroll in the lazy list.
 * @param itemIndex a lookup function for index of an item in the list relative to [itemsAvailable].
 */
@Composable
fun LazyListState.rememberScrollbarState(
    itemsAvailable: Int,
    itemIndex: (LazyListItemInfo) -> Int? = LazyListItemInfo::index,
): ScrollbarState =
    rememberScrollbarState(
        itemsAvailable = itemsAvailable,
        visibleItems = { layoutInfo.visibleItemsInfo },
        viewportSize = { layoutInfo.orientation.dimension(layoutInfo.viewportSize) },
        maxItemSize = { it.size },
        itemIndex = itemIndex
    )

/**
 * Remembers a [ScrollbarState] driven by the changes in a [LazyGridState]
 *
 * @param itemsAvailable the total amount of items available to scroll in the grid.
 * @param itemIndex a lookup function for index of an item in the grid relative to [itemsAvailable].
 */
@Composable
fun LazyGridState.rememberScrollbarState(
    itemsAvailable: Int,
    itemIndex: (LazyGridItemInfo) -> Int? = LazyGridItemInfo::index,
): ScrollbarState =
    rememberScrollbarState(
        itemsAvailable = itemsAvailable,
        visibleItems = { layoutInfo.visibleItemsInfo },
        viewportSize = { layoutInfo.orientation.dimension(layoutInfo.viewportSize) },
        maxItemSize = { layoutInfo.orientation.dimension(it.size) },
        itemIndex = itemIndex
    )

/**
 * Remembers a [ScrollbarState] driven by the changes in a [LazyStaggeredGridState]
 *
 * @param itemsAvailable the total amount of items available to scroll in the staggered grid.
 * @param itemIndex a lookup function for index of an item in the staggered grid relative
 * to [itemsAvailable].
 */
@Composable
fun LazyStaggeredGridState.rememberScrollbarState(
    itemsAvailable: Int,
    itemIndex: (LazyStaggeredGridItemInfo) -> Int? = LazyStaggeredGridItemInfo::index,
): ScrollbarState =
    rememberScrollbarState(
        itemsAvailable = itemsAvailable,
        visibleItems = { layoutInfo.visibleItemsInfo },
        viewportSize = { layoutInfo.orientation.dimension(layoutInfo.viewportSize) },
        maxItemSize = { layoutInfo.orientation.dimension(it.size) },
        itemIndex = itemIndex
    )

/**
 * A generic function for remembering [ScrollbarState] for lazy layouts.
 * @param itemsAvailable the total amount of items available to scroll in the layout.
 * @param visibleItems a list of items currently visible in the layout.
 * @param viewportSize a lookup function for the size of the view port in the [Orientation] axis
 * of the layout.
 * @param maxItemSize a lookup function for the visible item with the maximum size in the
 * [Orientation] axis of the layout.
 * @param itemIndex a lookup function for index of an item in the layout relative to [itemsAvailable].
 */
@Composable
// TODO: This utility function should be internal, but if it was, it would not be inline.
//  is this abstraction worthwhile? The code can be copy pasted for the lazy states that use it.
private fun <LazyState : ScrollableState, LazyStateItem> LazyState.rememberScrollbarState(
    itemsAvailable: Int,
    visibleItems: LazyState.() -> List<LazyStateItem>,
    viewportSize: LazyState.() -> Int,
    maxItemSize: LazyState.(LazyStateItem) -> Int,
    itemIndex: (LazyStateItem) -> Int?,
): ScrollbarState {
    var state by remember { mutableStateOf(ScrollbarState.FULL) }
    LaunchedEffect(
        key1 = this,
        key2 = itemsAvailable
    ) {
        snapshotFlow {
            val visibleItemsInfo = visibleItems(this@rememberScrollbarState)
            if (visibleItemsInfo.isEmpty()) return@snapshotFlow null

            val firstVisibleItem = visibleItemsInfo.firstOrNull()
                ?: return@snapshotFlow null

            val firstVisibleIndex = itemIndex(firstVisibleItem)
                ?.takeIf { it >= 0 }
                ?: return@snapshotFlow null

            val maxSize = visibleItemsInfo.maxOfOrNull { item ->
                maxItemSize(this@rememberScrollbarState, item)
            }
                ?.takeIf { it > 0 }
                ?: return@snapshotFlow null

            val itemsVisible = viewportSize(this@rememberScrollbarState).toFloat() / maxSize

            if (itemsAvailable != 0) ScrollbarState(
                thumbSizePercent = itemsVisible / itemsAvailable,
                thumbTravelPercent = firstVisibleIndex.toFloat() / itemsAvailable
            )
            else null
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { state = it }
    }
    return state
}

/**
 * Remembers a function to react to [Scrollbar] thumb position movements for a [ScrollbarState]
 */
@Composable
fun ScrollState.thumbInteractions(): (Float) -> Unit {
    var percentage by remember { mutableStateOf(Float.NaN) }

    LaunchedEffect(percentage) {
        if (percentage.isNaN()) return@LaunchedEffect
        scrollTo((percentage * maxValue).toInt())
    }
    return remember {
        { percentage = it }
    }
}

/**
 * Remembers a function to react to [Scrollbar] thumb position movements for a [LazyListState]
 * @param itemsAvailable the amount of items in the list.
 */
@Composable
fun LazyListState.thumbInteractions(
    itemsAvailable: Int,
): (Float) -> Unit = thumbInteractions(
    itemsAvailable = itemsAvailable,
    scroll = ::scrollToItem
)

/**
 * Remembers a function to react to [Scrollbar] thumb position movements for a [LazyGridState]
 * @param itemsAvailable the amount of items in the grid.
 */
@Composable
fun LazyGridState.thumbInteractions(
    itemsAvailable: Int,
): (Float) -> Unit = thumbInteractions(
    itemsAvailable = itemsAvailable,
    scroll = ::scrollToItem
)

/**
 * Remembers a function to react to [Scrollbar] thumb position movements for a [LazyStaggeredGridState]
 * @param itemsAvailable the amount of items in the grid.
 */
@Composable
fun LazyStaggeredGridState.thumbInteractions(
    itemsAvailable: Int,
): (Float) -> Unit = thumbInteractions(
    itemsAvailable = itemsAvailable,
    scroll = ::scrollToItem
)

/**
 * Generic function to react to [Scrollbar] thumb interactions in a lazy layout.
 * @param itemsAvailable the total amount of items available to scroll in the layout.
 * @param scroll a function to be invoked when an index has been identified to scroll to.
 */
@Composable
private fun thumbInteractions(
    itemsAvailable: Int,
    scroll: suspend (index: Int) -> Unit
): (Float) -> Unit {
    var percentage by remember { mutableStateOf(Float.NaN) }
    val itemCount by rememberUpdatedState(itemsAvailable)

    LaunchedEffect(percentage) {
        if (percentage.isNaN()) return@LaunchedEffect
        val indexToFind = (itemCount * percentage).toInt()
        scroll(indexToFind)
    }
    return remember {
        { percentage = it }
    }
}