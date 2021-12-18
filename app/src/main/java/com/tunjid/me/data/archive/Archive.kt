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

package com.tunjid.me.data.archive

import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(ArchiveKindSerializer::class)
enum class ArchiveKind(val type: String) {
    Articles("articles"),
    Projects("projects"),
    Talks("talks"),
}

@Serializable
data class Archive(
    val key: String,
    val link: String,
    val title: String,
    val body: String,
    val description: String,
    val thumbnail: String?,
    val author: User,
    @Serializable(LocalDateTimeSerializer::class)
    val created: Instant,
    val tags: List<String>,
    val categories: List<String>,
    val kind: ArchiveKind,
)

@Serializable
data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val imageUrl: String,
)

private object LocalDateTimeSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Instant) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Instant = decoder.decodeString().toInstant()
}

private object ArchiveKindSerializer : KSerializer<ArchiveKind> {
    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: ArchiveKind) =
        encoder.encodeString(value.type)

    override fun deserialize(decoder: Decoder): ArchiveKind = when (decoder.decodeString()) {
        ArchiveKind.Articles.type -> ArchiveKind.Articles
        ArchiveKind.Projects.type -> ArchiveKind.Projects
        ArchiveKind.Talks.type -> ArchiveKind.Talks
        else -> throw IllegalArgumentException("Unknown kind")
    }
}
