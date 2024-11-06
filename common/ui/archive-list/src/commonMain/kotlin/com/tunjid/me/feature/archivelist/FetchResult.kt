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
import com.tunjid.tiler.transform

val TiledList<ArchiveQuery, ArchiveItem.Card>.itemsWithHeaders: TiledList<ArchiveQuery, ArchiveItem>
    get() {
        val queriedArchives = this@itemsWithHeaders
        var year = -1
        return queriedArchives.transform { index ->
            val item = queriedArchives[index]
            val query = queriedArchives.queryAt(index)
            when (item) {
                is ArchiveItem.Card.Loaded -> {
                    val dateTime = item.archive.dateTime
                    if (year != dateTime.year) {
                        year = dateTime.year
                        add(
                            query = query,
                            item = ArchiveItem.Header(
                                index = item.index,
                                text = item.headerText,
                                // Keep the header key inconsistent between queries
                                // This is so scroll is anchored to the cards and not the header
                                key = "header-${item.headerText}-${query.hashCode()}"
                            )
                        )
                    }
                    add(
                        query = query,
                        item = item
                    )
                }

                is ArchiveItem.Card.PlaceHolder -> add(
                    query = query,
                    item = item
                )
            }
        }
    }
