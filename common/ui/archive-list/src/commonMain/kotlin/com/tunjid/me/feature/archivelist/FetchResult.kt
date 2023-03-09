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
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.buildTiledList
import com.tunjid.tiler.filterTransform

data class FetchResult(
    val query: ArchiveQuery,
    val archivesAvailable: Long,
    val queriedArchives: TiledList<ArchiveQuery, ArchiveItem>
)

val FetchResult.itemsWithHeaders: TiledList<ArchiveQuery, ArchiveItem>
    get() = buildTiledList {
        var month = -1
        var year = -1
        queriedArchives.forEachIndexed { index, item ->
            when (item) {
                is ArchiveItem.Loaded -> {
                    val query = queriedArchives.queryAt(index)
                    val dateTime = item.archive.dateTime
                    if (month != dateTime.monthNumber || year != dateTime.year) {
                        month = dateTime.monthNumber
                        year = dateTime.year
                        add(
                            query = query,
                            item = ArchiveItem.Header(
                                index = item.index,
                                text = item.headerText
                            )
                        )
                    }
                    add(
                        query = query,
                        item = item
                    )
                }

                is ArchiveItem.PlaceHolder -> add(
                    query = query,
                    item = item
                )
                else -> Unit
            }
        }
    }
