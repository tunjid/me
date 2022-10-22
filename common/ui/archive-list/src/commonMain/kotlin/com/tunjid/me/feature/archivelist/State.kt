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

import com.tunjid.me.core.model.Archive
import com.tunjid.me.core.model.ArchiveQuery
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.scaffold.nav.NavMutation
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val shouldScrollToTop: Boolean = true,
    val isInNavRail: Boolean = false,
    val hasFetchedAuthStatus: Boolean = false,
    val isSignedIn: Boolean = false,
    val queryState: QueryState,
    @Transient
    val lastVisibleKey: Any? = null,
    @Transient
    val items: List<ArchiveItem> = listOf()
) : ByteSerializable

val State.stickyHeader: ArchiveItem.Header?
    get() = when (lastVisibleKey) {
        null -> null
        else -> when (val lastVisibleItem = items.find { it.key == lastVisibleKey }) {
            is ArchiveItem.Header -> lastVisibleItem
            is ArchiveItem.Result -> ArchiveItem.Header(
                text = lastVisibleItem.headerText,
                query = lastVisibleItem.query
            )

            else -> null
        }
    }

sealed class Action(val key: String) {
    sealed class Fetch : Action(key = "Fetch") {
        abstract val query: ArchiveQuery
        abstract val gridSize: Int

        data class Reset(
            override val query: ArchiveQuery,
            override val gridSize: Int,
        ) : Fetch()

        data class LoadMore(
            override val query: ArchiveQuery,
            override val gridSize: Int,
        ) : Fetch()
    }

    data class FilterChanged(
        val descriptor: Descriptor
    ) : Action(key = "FilterChanged")

    data class GridSize(val size: Int) : Action(key = "GridSize")

    data class ToggleFilter(val isExpanded: Boolean? = null) : Action(key = "ToggleFilter")

    data class LastVisibleKey(val itemKey: Any) : Action(key = "LastVisibleKey")

    data class Navigate(val navMutation: NavMutation) : Action(key = "Navigate")
}

sealed class ArchiveItem {
    abstract val query: ArchiveQuery

    data class Header(
        val text: String,
        override val query: ArchiveQuery,
    ) : ArchiveItem()

    data class Result(
        val archive: Archive,
        override val query: ArchiveQuery,
    ) : ArchiveItem()

    data class Loading(
        val isCircular: Boolean,
        override val query: ArchiveQuery,
    ) : ArchiveItem()
}

val ArchiveItem.key: String
    get() = when (this) {
        is ArchiveItem.Header -> "header-${query.offset}-$text"
        is ArchiveItem.Loading -> "loading-${query.offset}"
        is ArchiveItem.Result -> "result-${query.offset}-${archive.id}"
    }

val Any.queryOffsetFromKey: Int?
    get() = when (this) {
        is String -> split("-").getOrNull(1)?.toIntOrNull()
        else -> null
    }

val Any.isHeaderKey: Boolean
    get() = this is String && this.startsWith("header")

val ArchiveItem.Result.prettyDate: String
    get() {
        val dateTime = archive.created.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dateTime.month.name} ${dateTime.dayOfMonth} ${dateTime.year}"
    }

val ArchiveItem.Result.readTime get() = "${archive.body.trim().split("/\\s+/").size / 250} min read"

val ArchiveItem.Result.dateTime
    get() = archive.created.toLocalDateTime(
        TimeZone.currentSystemDefault()
    )
val ArchiveItem.Result.headerText
    get(): String = with(dateTime) {
        "${month.name.lowercase().replaceFirstChar(Char::uppercase)}, $year"
    }

@Serializable
data class QueryState(
    val gridSize: Int = 1,
    val expanded: Boolean = false,
    val startQuery: ArchiveQuery,
    val currentQuery: ArchiveQuery,
    val categoryText: Descriptor.Category = Descriptor.Category(""),
    val tagText: Descriptor.Tag = Descriptor.Tag(""),
)

