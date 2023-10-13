/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.CollectionInfo

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberLazyStaggeredGridSemanticState(
    state: LazyStaggeredGridState,
    reverseScrolling: Boolean
): com.tunjid.me.core.ui.lazy.layout.LazyLayoutSemanticState =
    remember(state, reverseScrolling) {
        object : com.tunjid.me.core.ui.lazy.layout.LazyLayoutSemanticState {
            override val firstVisibleItemScrollOffset: Int
                get() = state.firstVisibleItemScrollOffset
            override val firstVisibleItemIndex: Int
                get() = state.firstVisibleItemIndex
            override val canScrollForward: Boolean
                get() = state.canScrollForward

            override suspend fun animateScrollBy(delta: Float) {
                state.animateScrollBy(delta)
            }

            override suspend fun scrollToItem(index: Int) {
                state.scrollToItem(index)
            }

            override fun collectionInfo(): CollectionInfo =
                CollectionInfo(-1, -1)
        }
    }
