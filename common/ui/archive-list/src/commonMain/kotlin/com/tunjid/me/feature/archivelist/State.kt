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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.tunjid.me.core.model.Archive
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveQuery
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.ui.ChipInfo
import com.tunjid.me.core.ui.ChipKind
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.scaffold.nav.NavMutation
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.buildTiledList
import com.tunjid.tiler.emptyTiledList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class State(
    @ProtoNumber(1)
    val shouldScrollToTop: Boolean = true,
    @ProtoNumber(2)
    val isInMainNav: Boolean = false,
    @ProtoNumber(3)
    val hasFetchedAuthStatus: Boolean = false,
    @ProtoNumber(4)
    val isSignedIn: Boolean = false,
    @ProtoNumber(5)
    val queryState: QueryState,
//    @ProtoNumber(6) Deprecated key
    @Transient
    val items: TiledList<ArchiveQuery, ArchiveItem> = emptyTiledList(),
) : ByteSerializable

val ArchiveItem.stickyHeader: ArchiveItem.Header?
    get() = when (this) {
        is ArchiveItem.Header -> this
        is ArchiveItem.Loaded -> ArchiveItem.Header(
            index = index,
            text = headerText
        )

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
        val descriptor: Descriptor,
    ) : Action(key = "FilterChanged")

    data class ToggleFilter(val isExpanded: Boolean? = null) : Action(key = "ToggleFilter")

    data class Navigate(val navMutation: NavMutation) : Action(key = "Navigate")
}

sealed class ArchiveItem {

    abstract val index: Int

    data class Header(
        override val index: Int,
        val text: String,
    ) : ArchiveItem()

    data class Loaded(
        override val index: Int,
        val key: String,
        val archive: Archive,
    ) : ArchiveItem()

    data class PlaceHolder(
        override val index: Int,
        val key: String,
        val lastId: ArchiveId? = null,
        val archive: Archive,
    ) : ArchiveItem()

    data class Loading(
        override val index: Int,
        val queryId: Int,
        val isCircular: Boolean,
    ) : ArchiveItem()
}

val ArchiveItem.key: String
    get() = when (this) {
        is ArchiveItem.Header -> "header-$text"
        // The ids have to match across these for fast scrolling to work
        is ArchiveItem.Loading -> "archive-$index-$queryId"
        is ArchiveItem.PlaceHolder -> key
        is ArchiveItem.Loaded -> key
    }

val Any.isHeaderKey: Boolean
    get() = this is String && this.startsWith("header")

val Archive.prettyDate: String
    get() {
        val dateTime = created.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dateTime.month.name} ${dateTime.dayOfMonth} ${dateTime.year}"
    }

val Archive.readTime get() = "${body.trim().split("/\\s+/").size / 250} min read"

val Archive.dateTime
    get() = created.toLocalDateTime(
        TimeZone.currentSystemDefault()
    )
val ArchiveItem.Loaded.headerText
    get(): String = with(archive.dateTime) {
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
    @Transient
    val suggestedDescriptors: List<Descriptor> = emptyList(),
)

@Composable
inline fun <reified T : Descriptor> Archive.descriptorChips(
    query: ArchiveQuery,
) =
    when (T::class) {
        Descriptor.Tag::class -> tags.map { it to query.contentFilter.tags.contains(it) }
        Descriptor.Category::class -> categories.map { it to query.contentFilter.categories.contains(it) }
        else -> throw IllegalArgumentException("Invalid descriptor class: ${T::class.qualifiedName}")
    }.map { (descriptor, selected) ->
        ChipInfo(
            text = descriptor.value,
            kind = ChipKind.Filter(
                selected = selected,
                tint = descriptor.tint()
            )
        )
    }

@Composable
inline fun <reified T : Descriptor> ArchiveQuery.descriptorChips() =
    when (T::class) {
        Descriptor.Tag::class -> contentFilter.tags
        Descriptor.Category::class -> contentFilter.categories
        else -> throw IllegalArgumentException("Invalid descriptor class")
    }.map {
        ChipInfo(
            text = it.value,
            kind = ChipKind.Input(
                selected = true,
                tint = it.tint()
            )
        )
    }

@Composable
fun QueryState.suggestedDescriptorChips() =
    suggestedDescriptors.map {
        ChipInfo(
            key = it,
            text = it.value,
            kind = ChipKind.Suggestion(
                tint = it.tint()
            )
        )
    }

@Composable
fun Descriptor.tint() = when (this) {
    is Descriptor.Category -> MaterialTheme.colorScheme.secondaryContainer
    is Descriptor.Tag -> MaterialTheme.colorScheme.tertiaryContainer
}

fun TiledList<ArchiveQuery, ArchiveItem>.preserveKeys(
    oldList: TiledList<ArchiveQuery, ArchiveItem>
): TiledList<ArchiveQuery, ArchiveItem> =
    with(oldList.fold(KeyPreserver(), KeyPreserver::cacheKey)) {
        buildTiledList {
            this@preserveKeys.forEachIndexed { index, item ->
                add(
                    this@preserveKeys.queryAt(index),
                    restoreKey(item)
                )
            }
        }
    }

/**
 * Tracks keys between successive loads to maintain scroll position.
 * Implementation is cheap as only <<100 items are sent to the UI at any one time.
 */
private class KeyPreserver {
    private val archiveIdsToKeys: MutableMap<ArchiveId, String> = mutableMapOf()

    fun cacheKey(item: ArchiveItem): KeyPreserver = when (item) {
        is ArchiveItem.Header,
        is ArchiveItem.Loading,
        is ArchiveItem.PlaceHolder -> Unit

        is ArchiveItem.Loaded -> archiveIdsToKeys[item.archive.id] = item.key
    }.let { this }

    fun restoreKey(item: ArchiveItem) = when (item) {
        is ArchiveItem.Header,
        is ArchiveItem.Loading,
        is ArchiveItem.PlaceHolder -> item

        is ArchiveItem.Loaded -> when (val key = archiveIdsToKeys.remove(item.archive.id)) {
            null -> item
            else -> item.copy(key = key)
        }
    }
}
