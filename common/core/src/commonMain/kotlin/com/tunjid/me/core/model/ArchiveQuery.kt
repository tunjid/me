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

package com.tunjid.me.core.model

import com.tunjid.me.core.model.Descriptor.Category
import com.tunjid.me.core.model.Descriptor.Tag
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

const val ARCHIVE_QUERY_LIMIT = 10

@Serializable
data class
ArchiveQuery(
    val kind: ArchiveKind,
    val temporalFilter: ArchiveTemporalFilter? = null,
    val contentFilter: ArchiveContentFilter = ArchiveContentFilter(),
    val desc: Boolean = true,
    val offset: Int = 0,
    val limit: Int = ARCHIVE_QUERY_LIMIT,
)

val ArchiveQuery.hasContentFilter
    get() = contentFilter.categories.isNotEmpty() || contentFilter.tags.isNotEmpty()

@Serializable
data class ArchiveTemporalFilter(
    val year: Int?,
    val month: Int?,
)

@Serializable
data class ArchiveContentFilter(
    val tags: List<Tag> = listOf(),
    val categories: List<Category> = listOf(),
)

@Serializable
sealed class Descriptor {
    abstract val value: String

    @Serializable(CategorySerializer::class)
    data class Category(override val value: String) : Descriptor()

    @Serializable(TagSerializer::class)
    data class Tag(override val value: String) : Descriptor()
}

operator fun ArchiveQuery.plus(descriptor: Descriptor) = amend(descriptor, List<Descriptor>::plus)

operator fun ArchiveQuery.minus(descriptor: Descriptor) = amend(descriptor, List<Descriptor>::minus)

private fun ArchiveQuery.amend(
    descriptor: Descriptor,
    operator: (List<Descriptor>, Descriptor) -> List<Descriptor>,
) = copy(
    contentFilter = contentFilter.copy(
        categories = when (descriptor) {
            is Category -> operator(contentFilter.categories, descriptor)
                .distinct()
                .filterIsInstance<Category>()

            else -> contentFilter.categories
        },
        tags = when (descriptor) {
            is Tag -> operator(contentFilter.tags, descriptor)
                .distinct()
                .filterIsInstance<Tag>()

            else -> contentFilter.tags
        }
    )
)

fun ArchiveQuery.compare(a: Archive, b: Archive) =
    if (desc) b.created.compareTo(a.created)
    else a.created.compareTo(b.created)

fun ArchiveQuery.includes(
    archive: Archive
): Boolean = kind == archive.kind
        && contentFilter.categories.intersect(archive.categories.toSet()).isNotEmpty()
        && contentFilter.tags.intersect(archive.tags.toSet()).isNotEmpty()
// TODO temporal filter

fun ArchiveQuery.hasTheSameFilter(
    other: ArchiveQuery
) = kind == other.kind
        && desc == other.desc
        && temporalFilter == other.temporalFilter
        && contentFilter.tags.toSet() == other.contentFilter.tags.toSet()
        && contentFilter.categories.toSet() == other.contentFilter.categories.toSet()

private class CategorySerializer : KSerializer<Category> by descriptorSerializer(
    creator = Descriptor::Category
)

private class TagSerializer : KSerializer<Tag> by descriptorSerializer(
    creator = Descriptor::Tag
)

private fun <T : Descriptor> descriptorSerializer(creator: (String) -> T) =
    object : KSerializer<T> {
        override val descriptor = PrimitiveSerialDescriptor("Descriptor", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: T) =
            encoder.encodeString(value.value)

        override fun deserialize(decoder: Decoder): T = creator(decoder.decodeString())
    }
