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

package com.tunjid.me.data.network.models

import com.tunjid.me.common.data.ArchiveEntity
import com.tunjid.me.common.data.UserEntity
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.model.UserId
import com.tunjid.me.core.utilities.LocalDateTimeSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class NetworkArchive(
    @SerialName("_id")
    val id: ArchiveId,
    val link: String,
    val title: String,
    val body: String,
    val description: String,
    val thumbnail: String?,
    val videoUrl: String?,
    val author: UserId,
    val likes: Long,
    @Serializable(LocalDateTimeSerializer::class)
    val created: Instant,
    val tags: List<Descriptor.Tag>,
    val categories: List<Descriptor.Category>,
    val kind: ArchiveKind,
)

fun NetworkArchive.toEntity() = ArchiveEntity(
    id = id.value,
    body = body,
    thumbnail = thumbnail,
    videoUrl = videoUrl,
    description = description,
    title = title,
    author = author.value,
    created = created.toEpochMilliseconds(),
    kind = kind.type,
    link = link,
    likes = likes,
)

fun NetworkArchive.authorShell() = UserEntity(
    id = author.value, first_name = "",
    last_name = "",
    full_name = "",
    image_url = "",
)