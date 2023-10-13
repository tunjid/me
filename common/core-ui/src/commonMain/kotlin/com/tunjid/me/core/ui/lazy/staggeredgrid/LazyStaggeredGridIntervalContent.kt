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
import androidx.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.runtime.Composable

@OptIn(ExperimentalFoundationApi::class)
internal class LazyStaggeredGridIntervalContent(
    content: LazyStaggeredGridScope.() -> Unit
) : LazyStaggeredGridScope, LazyLayoutIntervalContent<LazyStaggeredGridInterval>() {

    override val intervals = MutableIntervalList<LazyStaggeredGridInterval>()

    val spanProvider = LazyStaggeredGridSpanProvider(intervals)

    init {
        apply(content)
    }

    override fun item(
        key: Any?,
        contentType: Any?,
        span: StaggeredGridItemSpan?,
        content: @Composable LazyStaggeredGridItemScope.() -> Unit
    ) {
        items(
            count = 1,
            key = key?.let { { key } },
            contentType = { contentType },
            span = span?.let { { span } },
            itemContent = { content() }
        )
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        span: ((index: Int) -> StaggeredGridItemSpan)?,
        itemContent: @Composable LazyStaggeredGridItemScope.(index: Int) -> Unit
    ) {
        intervals.addInterval(
            count,
            LazyStaggeredGridInterval(
                key,
                contentType,
                span,
                itemContent
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class LazyStaggeredGridInterval(
    override val key: ((index: Int) -> Any)?,
    override val type: ((index: Int) -> Any?),
    val span: ((index: Int) -> StaggeredGridItemSpan)?,
    val item: @Composable LazyStaggeredGridItemScope.(Int) -> Unit
) : LazyLayoutIntervalContent.Interval
