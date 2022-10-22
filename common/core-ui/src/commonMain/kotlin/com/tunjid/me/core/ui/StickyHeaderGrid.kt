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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun StickyHeaderGrid(
    modifier: Modifier = Modifier,
    lazyState: LazyGridState,
    headerMatcher: (LazyGridItemInfo) -> Boolean,
    stickyHeader: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    var headerOffset by remember { mutableStateOf(0) }

    Box(modifier = modifier) {
        content()
        Box(
            modifier = Modifier.offset {
                IntOffset(
                    x = 0,
                    y = headerOffset
                )
            }
        ) {
            stickyHeader()
        }
    }
    LaunchedEffect(lazyState) {
        snapshotFlow {
            val layoutInfo = lazyState.layoutInfo
            val startOffset = layoutInfo.viewportStartOffset
            val firstCompletelyVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull {
                it.offset.y >= startOffset
            } ?: return@snapshotFlow 0

            when (headerMatcher(firstCompletelyVisibleItem)) {
                false -> 0
                true -> firstCompletelyVisibleItem.size
                    .height
                    .minus(firstCompletelyVisibleItem.offset.y)
                    .let { difference -> if (difference < 0) 0 else -difference }
            }
        }
            .distinctUntilChanged()
            .collect {
            println("headerOffset: $it")
                headerOffset = it }
    }
}