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
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveQuery
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.model.hasTheSameFilter
import com.tunjid.me.core.ui.ChipInfo
import com.tunjid.me.core.ui.ChipKind
import com.tunjid.me.core.ui.lazy.staggeredgrid.LazyStaggeredGridState
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.scaffold.navigation.NavigationAction
import com.tunjid.me.scaffold.navigation.NavigationMutation
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.tiler.filterIsInstance
import com.tunjid.tiler.map
import com.tunjid.tiler.queryAtOrNull
import com.tunjid.tiler.tiles
import com.tunjid.treenav.current
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.routeString
import com.tunjid.treenav.swap
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class State(
    @ProtoNumber(2)
    val isInPrimaryNav: Boolean = false,
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
    val listState: LazyStaggeredGridState? = null,
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
    ) : this(
        firstVisibleItemIndex.toLong()
            .shl(32) or (firstVisibleItemScrollOffset.toLong() and 0xFFFFFFFF)
    )

    internal val firstVisibleItemIndex get() = packedValue.shr(32).toInt()

    internal val firstVisibleItemScrollOffset get() = packedValue.and(0xFFFFFFFF).toInt()

    fun initialListState() = LazyStaggeredGridState(
        initialFirstVisibleItemIndex = firstVisibleItemIndex,
        initialFirstVisibleItemOffset = firstVisibleItemScrollOffset,
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
        data class LoadAround(val query: ArchiveQuery) : Fetch()

        data class NoColumnsChanged(val noColumns: Int) : Fetch()

        sealed class QueryChange : Fetch() {
            data class AddDescriptor(val descriptor: Descriptor) : QueryChange()
            data class RemoveDescriptor(val descriptor: Descriptor) : QueryChange()

            data class ToggleCategory(val category: Descriptor.Category) : QueryChange()

            data object ClearDescriptors : QueryChange()

            data object ToggleOrder : QueryChange()
        }
    }

    data class FilterChanged(
        val descriptor: Descriptor,
    ) : Action(key = "FilterChanged")

    data class ListStateChanged(
        val firstVisibleItemIndex: Int,
        val firstVisibleItemScrollOffset: Int,
    ) : Action(key = "ListStateChanged")

    data class ToggleFilter(val isExpanded: Boolean? = null) : Action(key = "ToggleFilter")

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {

        data class Detail(val archive: Archive) : Navigate() {
            override val navigationMutation: NavigationMutation = {
                val route = routeString(
                    path = "archives/${archive.kind.type}/${archive.id.value}",
                    queryParams = mapOf(
                        "thumbnail" to listOfNotNull(archive.thumbnail),
                    )
                ).toRoute
                if (navState.current is ArchiveListRoute) navState.push(node = route)
                else navState.swap(node = route)
            }
        }

        data class Files(
            val archiveId: ArchiveId,
            val thumbnail: String?,
            val kind: ArchiveKind
        ) : Navigate() {
            override val navigationMutation: NavigationMutation = {
                navState.push(
                    routeString(
                        path = "archives/${kind.type}/${archiveId.value}/files/image",
                        queryParams = mapOf("url" to listOfNotNull(thumbnail))
                    ).toRoute
                )
            }
        }

        data class Generic(
            override val navigationMutation: NavigationMutation
        ) : Navigate()
    }
}

sealed class ArchiveItem(val contentType: String) {

    abstract val index: Int
    abstract val key: String

    data class Header(
        override val index: Int,
        override val key: String,
        val text: String,
    ) : ArchiveItem("Header")

    data class Loading(
        override val index: Int,
        override val key: String,
        val isCircular: Boolean,
    ) : ArchiveItem("Loading")

    sealed class Card(contentType: String) : ArchiveItem(contentType) {

        abstract val archive: Archive

        data class Loaded(
            override val index: Int,
            override val key: String,
            override val archive: Archive,
        ) : Card("Loaded")

        data class PlaceHolder(
            override val index: Int,
            override val key: String,
            override val archive: Archive,
        ) : Card("Placeholder")
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
//        "${month.name.lowercase().replaceFirstChar(Char::uppercase)}, $year"
        "$year"
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
        Descriptor.Category::class -> categories.map {
            it to query.contentFilter.categories.contains(
                it
            )
        }

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
    newItems: TiledList<ArchiveQuery, ArchiveItem.Card>,
): TiledList<ArchiveQuery, ArchiveItem.Card> {
    val oldQuery = items.queryAtOrNull(0)
    val newQuery = newItems.queryAtOrNull(0) ?: return emptyTiledList()
    val hasSameFilter = oldQuery != null && newQuery.hasTheSameFilter(oldQuery)

    // Simple pagination, accept all new items
    if (hasSameFilter) return newItems

    // A filter change, placeholders are unnecessary as content already exists. Wait till
    // actual content is loaded
    if (newItems.hasPlaceholders()) return items.filterIsInstance<ArchiveQuery, ArchiveItem.Card>()

    // Actual content has been loaded, preserve the ids of items that existed before.
    val oldArchiveIdsToKeys = mutableMapOf<ArchiveId, String>().apply {
        for (it in items) if (it is ArchiveItem.Card.Loaded) set(it.archive.id, it.key)
    }

    return newItems.map { item ->
        when (item) {
            is ArchiveItem.Card.Loaded -> when (
                val existingKey = oldArchiveIdsToKeys.remove(item.archive.id)
            ) {
                null -> item
                else -> item.copy(key = existingKey)
            }

            is ArchiveItem.Card.PlaceHolder -> item
        }
    }
}

fun TiledList<ArchiveQuery, *>.hasPlaceholders() = tiles().any { tile ->
    get(tile.start) is ArchiveItem.Card.PlaceHolder
}