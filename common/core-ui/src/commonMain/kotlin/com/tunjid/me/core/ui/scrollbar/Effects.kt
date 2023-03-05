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
fun LazyListState.ScrollbarState(
    vararg keys: Any? = emptyArray(),
    size: Int,
    indexForItem: (LazyListItemInfo) -> Int?,
): ScrollbarState {
    var scrollbarState by remember { mutableStateOf(ScrollbarState.FULL) }
    LaunchedEffect(
        keys = *(arrayOf(this, size) + keys)
    ) {
        snapshotFlow {
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            val visible = visibleItemsInfo.size
            val info = visibleItemsInfo.lastOrNull()
            val index = info?.let(indexForItem)?.takeIf { it >= 0 } ?: return@snapshotFlow null

            ScrollbarState(
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

@Composable
fun LazyGridState.ScrollbarState(
    vararg keys: Any? = emptyArray(),
    size: Int,
    indexForItem: (LazyGridItemInfo) -> Int?,
): ScrollbarState {
    var scrollbarState by remember { mutableStateOf(ScrollbarState.FULL) }
    LaunchedEffect(
        keys = *(arrayOf(this, size) + keys)
    ) {
        snapshotFlow {
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            val visible = visibleItemsInfo.size
            val info = visibleItemsInfo.lastOrNull()
            val index = info?.let(indexForItem)?.takeIf { it >= 0 } ?: return@snapshotFlow null

            ScrollbarState(
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

@Composable
fun LazyStaggeredGridState.ScrollbarState(
    vararg keys: Any? = emptyArray(),
    size: Int,
    indexForItem: (LazyStaggeredGridItemInfo) -> Int?,
): ScrollbarState {
    var scrollbarState by remember { mutableStateOf(ScrollbarState.FULL) }
    LaunchedEffect(
        keys = *(arrayOf(this, size) + keys)
    ) {
        snapshotFlow {
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            val visible = visibleItemsInfo.size
            val info = visibleItemsInfo.lastOrNull()
            val index = info?.let(indexForItem)?.takeIf { it >= 0 } ?: return@snapshotFlow null

            ScrollbarState(
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