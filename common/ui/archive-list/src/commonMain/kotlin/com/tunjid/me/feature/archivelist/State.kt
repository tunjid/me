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

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.tunjid.me.core.model.Archive
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveQuery
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.model.hasTheSameFilter
import com.tunjid.me.core.model.includes
import com.tunjid.me.core.ui.ChipInfo
import com.tunjid.me.core.ui.ChipKind
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.scaffold.nav.NavMutation
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.buildTiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.tiler.tiledListOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class State(
    @ProtoNumber(2)
    val isInMainNav: Boolean = false,
    @ProtoNumber(3)
    val hasFetchedAuthStatus: Boolean = false,
    @ProtoNumber(4)
    val isSignedIn: Boolean = false,
    @ProtoNumber(5)
    val queryState: QueryState,
    @ProtoNumber(7)
    val savedListState: SavedListState = SavedListState(),
//    @ProtoNumber(6) Deprecated key
//    @ProtoNumber(1) Deprecated key
    @Transient
    val isLoading: Boolean = true,
    @Transient
    val listState: LazyGridState? = null,
    @Transient
    val items: TiledList<ArchiveQuery, ArchiveItem> = emptyTiledList(),
) : ByteSerializable

@Serializable
@JvmInline
value class SavedListState(
    private val packedValue: Long = 0,
) {
    constructor(
        firstVisibleItemIndex: Int = 0,
        firstVisibleItemScrollOffset: Int = 0,
    ) : this(firstVisibleItemIndex.toLong().shl(32) or (firstVisibleItemScrollOffset.toLong() and 0xFFFFFFFF))

    fun initialListState() = LazyGridState(
        firstVisibleItemIndex = packedValue.shr(32).toInt(),
        firstVisibleItemScrollOffset = packedValue.and(0xFFFFFFFF).toInt(),
    )
}

val ArchiveItem.stickyHeader: ArchiveItem.Header?
    get() = when (this) {
        is ArchiveItem.Header -> this
        is ArchiveItem.Card.Loaded -> ArchiveItem.Header(
            index = index,
            text = headerText,
            key = "header-$headerText",
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

    data class ListStateChanged(
        val firstVisibleItemIndex: Int,
        val firstVisibleItemScrollOffset: Int,
    ) : Action(key = "ListStateChanged")

    data class ToggleFilter(val isExpanded: Boolean? = null) : Action(key = "ToggleFilter")

    data class Navigate(val navMutation: NavMutation) : Action(key = "Navigate")
}

sealed class ArchiveItem {

    abstract val index: Int
    abstract val key: String

    data class Header(
        override val index: Int,
        override val key: String,
        val text: String,
    ) : ArchiveItem()

    data class Loading(
        override val index: Int,
        override val key: String,
        val isCircular: Boolean,
    ) : ArchiveItem()

    sealed class Card : ArchiveItem() {

        abstract val archive: Archive

        data class Loaded(
            override val index: Int,
            override val key: String,
            override val archive: Archive,
        ) : Card()

        data class PlaceHolder(
            override val index: Int,
            override val key: String,
            override val archive: Archive,
        ) : Card()
    }
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
val ArchiveItem.Card.Loaded.headerText
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

/**
 * Tracks keys between successive loads to maintain scroll position.
 * Implementation is cheap as only <<100 items are sent to the UI at any one time.
 */
fun State.preserveKeys(
    newQuery: ArchiveQuery,
    newList: TiledList<ArchiveQuery, ArchiveItem.Card>,
): TiledList<ArchiveQuery, ArchiveItem> = buildTiledList {
    val oldArchiveIdsToKeys = mutableMapOf<ArchiveId, String>()
    val oldLoadedItems = mutableListOf<ArchiveItem.Card.Loaded>()
    val hasSameFilter = newQuery.hasTheSameFilter(queryState.currentQuery)

    for (it in items) {
        if (it !is ArchiveItem.Card.Loaded) continue
        if (!hasSameFilter && !newQuery.includes(it.archive)) continue
        oldLoadedItems.add(it)
        oldArchiveIdsToKeys[it.archive.id] = it.key
    }

    val sortedLoadedItems = when {
        // No need to sort, old items are not being fed back into the results
        hasSameFilter -> mutableListOf()
        // Items will be read from the end to improve performance
        else -> when (newQuery.desc) {
            queryState.currentQuery.desc -> oldLoadedItems.asReversed()
            else -> oldLoadedItems
        }
    }
    var sortedSize = sortedLoadedItems.size

    try {
        for (index in 0 until newList.size) {
            val item = newList[index]
            if (item is ArchiveItem.Card.Loaded && !newQuery.includes(item.archive)) continue
            add(
                query = newList.queryAt(index),
                item = when (item) {
                    is ArchiveItem.Card.Loaded -> when (val existingKey = oldArchiveIdsToKeys.remove(item.archive.id)) {
                        null -> item
                        else -> item.copy(key = existingKey)
                    }

                    is ArchiveItem.Card.PlaceHolder -> {
                        // Replace placeholders with existing data that matches
                        sortedLoadedItems.getOrNull(--sortedSize)
                            ?.also { oldArchiveIdsToKeys.remove(it.archive.id) }
                            ?: item
                    }
                }
            )
        }
    }
    catch (e: Exception) {
        e.printStackTrace()
    }
}
