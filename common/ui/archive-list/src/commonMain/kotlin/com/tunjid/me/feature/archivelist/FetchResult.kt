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

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
                        val dateTime = item.archive.created.toLocalDateTime(
                            TimeZone.currentSystemDefault()
                        )
                        if (month != dateTime.monthNumber || year != dateTime.year) {
                            month = dateTime.monthNumber
                            year = dateTime.year
                            ArchiveItem.Header(
                                text = "${dateTime.month.name}, ${dateTime.year}",
                                query = item.query
                            )
                                .takeUnless {
                                    keySet.contains(it.key) && it.query.contentFilter == action.query.contentFilter
                                }
                                ?.also { keySet.add(it.key) }
                                ?.also(::add)
                        }

                        item
                            .takeUnless {
                                keySet.contains(it.key) && it.query.contentFilter == action.query.contentFilter
                            }
                            ?.also { keySet.add(it.key) }
                            ?.also(::add)
                    }
                }
            }
        }
    }


private val FetchResult.hasNoResults: Boolean
    get() = queriedArchives.isEmpty() || queriedArchives.all {
        it.all { items -> items is ArchiveItem.Loading }
    }


private val FetchResult.flattenedArchives: List<ArchiveItem>
    get() = queriedArchives
        .flatten()
        .distinctBy { it.key }

fun FetchResult.m(default: List<ArchiveItem>): List<ArchiveItem> = when {
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

            else -> flattenedArchives
        }
            // Filtering is cheap because at most 4 * [DefaultQueryLimit] items
            // are ever sent to the UI
            .filter { item ->
                when (item) {
                    is ArchiveItem.Header -> true
                    is ArchiveItem.Loading -> true
                    is ArchiveItem.Result -> item.query.contentFilter == action.query.contentFilter
                }
            }
