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
fun LazyListState.scrollbarState(
    itemsAvailable: Int,
    itemIndex: (LazyListItemInfo) -> Int = LazyListItemInfo::index,
): ScrollbarState =
    scrollbarState(
        itemsAvailable = itemsAvailable,
        visibleItems = { layoutInfo.visibleItemsInfo },
        viewportArea = { layoutInfo.orientation.dimension(layoutInfo.viewportSize) },
        maxItemArea = { it.size },
        itemOffset = { visibleItems ->
            offsetCalculator(
                visibleItems = visibleItems,
                maxItemSize = { it.size },
                offset = { it.offset },
                next = { first -> visibleItems.find { it != first } },
                itemIndex = itemIndex
            )
        },
        itemIndex = itemIndex
    )

/**
 * Remembers a [ScrollbarState] driven by the changes in a [LazyGridState]
 *
 * @param itemsAvailable the total amount of items available to scroll in the grid.
 * @param itemIndex a lookup function for index of an item in the grid relative to [itemsAvailable].
 */
@Composable
fun LazyGridState.scrollbarState(
    itemsAvailable: Int,
    itemIndex: (LazyGridItemInfo) -> Int = LazyGridItemInfo::index,
): ScrollbarState =
    scrollbarState(
        itemsAvailable = itemsAvailable,
        visibleItems = { layoutInfo.visibleItemsInfo },
        viewportArea = { layoutInfo.viewportSize.height * layoutInfo.viewportSize.width },
        maxItemArea = {
            when (layoutInfo.orientation) {
                Orientation.Vertical -> {
                    (it.size.height + layoutInfo.mainAxisItemSpacing) * it.size.width
                }
                Orientation.Horizontal -> {
                    it.size.height * (it.size.width + layoutInfo.mainAxisItemSpacing)
                }
            }
        },
        itemOffset = { visibleItems ->
            offsetCalculator(
                visibleItems = visibleItems,
                maxItemSize = { layoutInfo.orientation.dimension(it.size) },
                offset = { layoutInfo.orientation.dimension(it.offset) },
                next = { first ->
                    when (layoutInfo.orientation) {
                        Orientation.Vertical -> visibleItems.find {
                            it != first && it.row != first.row
                        }

                        Orientation.Horizontal -> visibleItems.find {
                            it != first && it.column != first.column
                        }
                    }
                },
                itemIndex = itemIndex
            )
        },
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
fun LazyStaggeredGridState.scrollbarState(
    itemsAvailable: Int,
    itemIndex: (LazyStaggeredGridItemInfo) -> Int = LazyStaggeredGridItemInfo::index,
): ScrollbarState =
    scrollbarState(
        itemsAvailable = itemsAvailable,
        visibleItems = { layoutInfo.visibleItemsInfo },
        viewportArea = { layoutInfo.orientation.dimension(layoutInfo.viewportSize) },
        itemOffset = { visibleItems ->
            offsetCalculator(
                visibleItems = visibleItems,
                maxItemSize = { layoutInfo.orientation.dimension(it.size) },
                offset = { layoutInfo.orientation.dimension(it.offset) },
                next = { first ->
                    visibleItems.find { it != first && it.lane == first.lane }
                },
                itemIndex = itemIndex
            )
        },
        maxItemArea = { layoutInfo.orientation.dimension(it.size) },
        itemIndex = itemIndex
    )
