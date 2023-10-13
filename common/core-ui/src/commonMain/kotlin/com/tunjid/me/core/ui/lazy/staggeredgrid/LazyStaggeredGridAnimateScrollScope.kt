/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.me.core.ui.lazy.staggeredgrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.ui.util.fastSumBy
import com.tunjid.me.core.ui.lazy.layout.LazyLayoutAnimateScrollScope
import kotlin.math.abs

@ExperimentalFoundationApi
internal class LazyStaggeredGridAnimateScrollScope(
    private val state: LazyStaggeredGridState
) : LazyLayoutAnimateScrollScope {

    override val firstVisibleItemIndex: Int get() = state.firstVisibleItemIndex

    override val firstVisibleItemScrollOffset: Int get() = state.firstVisibleItemScrollOffset

    override val lastVisibleItemIndex: Int
        get() = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

    override val itemCount: Int get() = state.layoutInfo.totalItemsCount

    override fun getVisibleItemScrollOffset(index: Int): Int {
        val searchedIndex = state.layoutInfo.visibleItemsInfo.binarySearch { it.index - index }
        val item = state.layoutInfo.visibleItemsInfo[searchedIndex]
        return item.offset.let {
            if (state.isVertical) it.y else it.x
        }
    }

    override fun ScrollScope.snapToItem(index: Int, scrollOffset: Int) {
        with(state) {
            snapToItemInternal(index, scrollOffset)
        }
    }

    override fun calculateDistanceTo(targetIndex: Int, targetItemOffset: Int): Float {
        val averageMainAxisItemSize = visibleItemsAverageSize

        val lineDiff = targetIndex / state.laneCount - firstVisibleItemIndex / state.laneCount
        var coercedOffset = minOf(abs(targetItemOffset), averageMainAxisItemSize)
        if (targetItemOffset < 0) coercedOffset *= -1
        return averageMainAxisItemSize * lineDiff.toFloat() +
            coercedOffset - firstVisibleItemScrollOffset
    }

    override suspend fun scroll(block: suspend ScrollScope.() -> Unit) {
        state.scroll(block = block)
    }

    override val visibleItemsAverageSize: Int
        get() {
            val layoutInfo = state.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val itemSizeSum = visibleItems.fastSumBy {
                if (state.isVertical) it.size.height else it.size.width
            }
            return itemSizeSum / visibleItems.size + layoutInfo.mainAxisItemSpacing
        }
}
