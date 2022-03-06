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

import com.tunjid.me.core.model.ArchiveKind.Articles
import com.tunjid.me.core.model.ArchiveKind.Projects
import com.tunjid.me.core.model.ArchiveKind.Talks
import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(ArchiveKindSerializer::class)
enum class ArchiveKind(val type: String) {
    Articles(type = "articles"),
    Projects(type = "projects"),
    Talks(type = "talks"),
}

val ArchiveKind.singular get() = when(this) {
    Articles -> "article"
    Projects -> "project"
    Talks -> "talks"
}

@Serializable
data class Archive(
    @SerialName("_id")
    val id: ArchiveId,
    val link: String,
    val title: String,
    val body: String,
    val description: String,
    val thumbnail: String?,
    val author: User,
    val likes: Long,
    @Serializable(LocalDateTimeSerializer::class)
    val created: Instant,
    val tags: List<Descriptor.Tag>,
    val categories: List<Descriptor.Category>,
    val kind: ArchiveKind,
)

@Serializable
data class User(
    @SerialName("_id")
    val id: UserId,
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

object ArchiveKindSerializer : KSerializer<ArchiveKind> {
    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: ArchiveKind) =
        encoder.encodeString(value.type)

    override fun deserialize(decoder: Decoder): ArchiveKind = when (decoder.decodeString()) {
        Articles.type -> Articles
        Projects.type -> Projects
        Talks.type -> Talks
        else -> throw IllegalArgumentException("Unknown kind")
    }
}
