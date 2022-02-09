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

package com.tunjid.me.common.ui.archive

import com.tunjid.me.common.AppAction
import com.tunjid.me.common.data.ByteSerializable
import com.tunjid.me.common.data.local.Archive
import com.tunjid.me.common.data.Model.ArchiveQuery
import com.tunjid.me.common.data.Model.Descriptor
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val gridSize: Int = 1,
    val shouldScrollToTop: Boolean = true,
    val isInNavRail: Boolean = false,
    val queryState: QueryState,
    @Transient
    val lastVisibleKey: Any? = null,
    @Transient
    val items: List<ArchiveItem> = listOf()
) : ByteSerializable

sealed class Action(val key: String) {
    sealed class Fetch : Action(key = "Fetch") {
        abstract val query: ArchiveQuery

        data class Reset(
            override val query: ArchiveQuery
        ) : Fetch()

        data class LoadMore(
            override val query: ArchiveQuery
        ) : Fetch()
    }

    data class FilterChanged(
        val descriptor: Descriptor
    ) : Action(key = "FilterChanged")

    data class Navigate(
        val navAction: AppAction.Nav
    ) : Action(key = "Navigate")

    data class GridSize(val size: Int) : Action(key = "GridSize")

    data class ToggleFilter(val isExpanded: Boolean? = null) : Action(key = "ToggleFilter")

    data class LastVisibleKey(val itemKey: Any) : Action(key = "LastVisibleKey")
}

sealed class ArchiveItem {
    abstract val query: ArchiveQuery

    data class Result(
        val archive: Archive,
        override val query: ArchiveQuery,
    ) : ArchiveItem()

    data class Loading(
        val isCircular: Boolean,
        override val query: ArchiveQuery,
    ) : ArchiveItem()
}

private class ItemKey(
    val key: String,
    val query: ArchiveQuery
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ItemKey

        if (key != other.key) return false
        if (query != other.query) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + query.hashCode()
        return result
    }
}

val ArchiveItem.key: Any
    get() = when (this) {
        is ArchiveItem.Loading -> ItemKey(
            key = "header-$query",
            query = query
        )
        is ArchiveItem.Result -> ItemKey(
            key = "result-${archive.id}",
            query = query
        )
    }

val Any.queryFromKey get() = if(this is ItemKey) this.query else null

val ArchiveItem.Result.prettyDate: String
    get() {
        val dateTime = archive.created.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dateTime.month.name} ${dateTime.monthNumber} ${dateTime.year}"
    }

val ArchiveItem.Result.readTime get() = "${archive.body.trim().split("/\\s+/").size / 250} min read"

@Serializable
data class QueryState(
    val expanded: Boolean = false,
    val startQuery: ArchiveQuery,
    val currentQuery: ArchiveQuery,
    val categoryText: Descriptor.Category = Descriptor.Category(""),
    val tagText: Descriptor.Tag = Descriptor.Tag(""),
)

