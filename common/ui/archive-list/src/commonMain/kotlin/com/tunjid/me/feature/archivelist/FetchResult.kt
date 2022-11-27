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

data class FetchResult(
    val action: Action.Fetch,
    val queriedArchives: List<List<ArchiveItem>>
)

fun FetchResult.items(default: List<ArchiveItem>) = when {
    hasNoResults -> when (action) {
        // Fetch action is reset, show a loading spinner
        is Action.Fetch.Reset -> listOf(
            ArchiveItem.Loading(
                isCircular = true,
                query = action.query
            )
        )
        // The mutator was just resubscribed to, show existing items
        else -> default
    }

    else -> items
}

private val FetchResult.items: List<ArchiveItem>
    get() {
        fun MutableList<ArchiveItem>.addIfNotPresent(
            keySet: MutableSet<String>,
            item: ArchiveItem
        ) {
            item.takeUnless {
                keySet.contains(it.key) && it.query.contentFilter == action.query.contentFilter
            }
                ?.also { keySet.add(it.key) }
                ?.also(::add)
        }

        var month = -1
        var year = -1
        val keySet = mutableSetOf<String>()
        return queriedArchives.flatMapIndexed { index, items ->
            when (index) {
                0, queriedArchives.lastIndex -> items
                else -> items.filterNot { it is ArchiveItem.Loading }
            }.flatMap { item ->
                buildList {
                    if (item is ArchiveItem.Result) {
                        val dateTime = item.dateTime
                        if (month != dateTime.monthNumber || year != dateTime.year) {
                            month = dateTime.monthNumber
                            year = dateTime.year
                            addIfNotPresent(
                                keySet = keySet,
                                item = ArchiveItem.Header(
                                    text = item.headerText,
                                    query = item.query
                                )
                            )
                        }
                        addIfNotPresent(
                            keySet = keySet,
                            item = item
                        )
                    }
                }
            }
        }
    }


private val FetchResult.hasNoResults: Boolean
    get() = queriedArchives.isEmpty() || queriedArchives.all {
        it.all { items -> items is ArchiveItem.Loading }
    }
