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
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class State(
    val shouldScrollToTop: Boolean = true,
    val isMainContent: Boolean = false,
    val hasFetchedAuthStatus: Boolean = false,
    val isSignedIn: Boolean = false,
    val queryState: QueryState,
    val lastVisibleKey: String? = null,
    @Transient
    val items: TiledList<ArchiveQuery, ArchiveItem> = emptyTiledList()
) : ByteSerializable

val ArchiveItem.stickyHeader: ArchiveItem.Header?
    get() = when (this) {
        is ArchiveItem.Header -> this
        is ArchiveItem.Result -> ArchiveItem.Header(text = headerText)
        else -> null
    }

sealed class Action(val key: String) {
    sealed class Fetch : Action(key = "Fetch") {

        sealed interface QueriedFetch {
            val query: ArchiveQuery
        }

        data class QueryChange(override val query: ArchiveQuery) : Fetch(), QueriedFetch

        data class LoadAround(override val query: ArchiveQuery) : Fetch(), QueriedFetch

        data class NoColumnsChanged(val noColumns: Int) : Fetch()
    }

    data class FilterChanged(
        val descriptor: Descriptor
    ) : Action(key = "FilterChanged")

    data class ToggleFilter(val isExpanded: Boolean? = null) : Action(key = "ToggleFilter")

    data class LastVisibleKey(val itemKey: String) : Action(key = "LastVisibleKey")

    data class Navigate(val navMutation: NavMutation) : Action(key = "Navigate")
}

sealed class ArchiveItem {

    data class Header(
        val text: String,
    ) : ArchiveItem()

    data class Result(
        val archive: Archive,
    ) : ArchiveItem()

    data class Loading(
        val isCircular: Boolean,
    ) : ArchiveItem()
}

val ArchiveItem.key: String
    get() = when (this) {
        is ArchiveItem.Header -> "header-$text"
        is ArchiveItem.Loading -> "loading"
        is ArchiveItem.Result -> "result-${archive.id}"
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
    // Deleted field
    // @ProtoNumber(1)
    // val gridSize: Int = 1,
    @ProtoNumber(2)
    val expanded: Boolean = false,
    @ProtoNumber(3)
    val currentQuery: ArchiveQuery,
    // Deleted field
    // val currentQuery: ArchiveQuery,
    @ProtoNumber(5)
    val categoryText: Descriptor.Category = Descriptor.Category(""),
    @ProtoNumber(6)
    val tagText: Descriptor.Tag = Descriptor.Tag(""),
    @ProtoNumber(7)
    val count: Long = 0,
)

