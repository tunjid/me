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

package com.tunjid.me.common.data.archive

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

const val DefaultQueryLimit = 6

@Serializable
data class ArchiveQuery(
    val kind: ArchiveKind,
    val temporalFilter: ArchiveTemporalFilter? = null,
    val contentFilter: ArchiveContentFilter = ArchiveContentFilter(),
    val offset: Int = 0,
    val limit: Int = DefaultQueryLimit
)

val ArchiveQuery.hasContentFilter
    get() = contentFilter.categories.isNotEmpty() || contentFilter.tags.isNotEmpty()

@Serializable
data class ArchiveTemporalFilter(
    val year: Int?,
    val month: Int?
)

@Serializable
data class ArchiveContentFilter(
    val tags: List<Descriptor.Tag> = listOf(),
    val categories: List<Descriptor.Category> = listOf(),
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
    operator: (List<Descriptor>, Descriptor) -> List<Descriptor>
) = copy(
    contentFilter = contentFilter.copy(
        categories = when (descriptor) {
            is Descriptor.Category -> operator(contentFilter.categories, descriptor)
                .distinct()
                .filterIsInstance<Descriptor.Category>()
            else -> contentFilter.categories
        },
        tags = when (descriptor) {
            is Descriptor.Tag -> operator(contentFilter.tags, descriptor)
                .distinct()
                .filterIsInstance<Descriptor.Tag>()
            else -> contentFilter.tags
        }
    )
)

private class CategorySerializer(
    backing: KSerializer<Descriptor.Category> = descriptorSerializer(Descriptor::Category)
) : KSerializer<Descriptor.Category> by backing

private class TagSerializer(
    backing: KSerializer<Descriptor.Tag> = descriptorSerializer(Descriptor::Tag)
) : KSerializer<Descriptor.Tag> by backing

private fun <T : Descriptor> descriptorSerializer(creator: (String) -> T) =
    object : KSerializer<T> {
        override val descriptor = PrimitiveSerialDescriptor("Descriptor", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: T) =
            encoder.encodeString(value.value).also { println("Serializing") }

        override fun deserialize(decoder: Decoder): T = creator(decoder.decodeString())
    }