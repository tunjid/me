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
import com.tunjid.me.common.data.ArchiveFileEntity
import com.tunjid.me.common.data.UserEntity
import com.tunjid.me.core.model.ArchiveFileId
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.model.UserId
import com.tunjid.me.core.utilities.LocalDateTimeSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class NetworkArchiveFile(
    @SerialName("_id")
    val id: ArchiveFileId,
    val url: String,
    val mimetype: String,
    val uploader: UserId,
    val archiveId: ArchiveId,
    @Serializable(LocalDateTimeSerializer::class)
    val created: Instant,
)

fun NetworkArchiveFile.toEntity() = ArchiveFileEntity(
    id = id.value,
    url = url,
    mimetype = mimetype,
    uploader = uploader.value,
    archive_id = archiveId.value,
    created = created.toEpochMilliseconds(),
)

fun NetworkArchiveFile.uploaderShell() = UserEntity(
    id = uploader.value,
    first_name = "",
    last_name = "",
    full_name = "",
    image_url = "",
)

fun NetworkArchiveFile.archiveShell() = ArchiveEntity(
    id = archiveId.value,
    link = "",
    title = "",
    body = "",
    description = "",
    thumbnail = "",
    videoUrl = "",
    author = uploader.value,
    likes = 0,
    created = 0,
    kind = ArchiveKind.Articles.type,
)
