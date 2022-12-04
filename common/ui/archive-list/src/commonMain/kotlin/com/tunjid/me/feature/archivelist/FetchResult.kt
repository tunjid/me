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

package com.tunjid.me.feature.archivelist

import com.tunjid.me.core.model.ArchiveQuery
import com.tunjid.tiler.MutableTiledList
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.buildTiledList
import com.tunjid.tiler.filterTransform

data class FetchResult(
    val action: Action.Fetch,
    val queriedArchives: TiledList<ArchiveQuery, ArchiveItem>
)

fun FetchResult.items(default: TiledList<ArchiveQuery, ArchiveItem>) = when {
    hasNoResults -> when (action) {
        // Fetch action is reset, show a loading spinner
        is Action.Fetch.Reset -> buildTiledList {
            add(
                query = action.query,
                item = ArchiveItem.Loading(isCircular = true)
            )
        }
        // The mutator was just resubscribed to, show existing items
        else -> default
    }

    else -> items
}

private val FetchResult.items: TiledList<ArchiveQuery, ArchiveItem>
    get() = buildTiledList {
        var month = -1
        var year = -1
        queriedArchives.forEachIndexed { index, item ->
            if (item is ArchiveItem.Result) {
                val query = queriedArchives.queryFor(index)
                val dateTime = item.dateTime
                if (month != dateTime.monthNumber || year != dateTime.year) {
                    month = dateTime.monthNumber
                    year = dateTime.year
                    add(
                        query = query,
                        item = ArchiveItem.Header(text = item.headerText)
                    )
                }
                add(
                    query = query,
                    item = item
                )
            }
        }
    }.filterTransform { distinctBy(ArchiveItem::key) }

private val FetchResult.hasNoResults: Boolean
    get() = queriedArchives.isEmpty() || queriedArchives.all { item ->
        item is ArchiveItem.Loading
    }
