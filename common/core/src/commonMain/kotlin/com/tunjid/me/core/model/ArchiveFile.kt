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

import com.tunjid.me.core.model.ArchiveKind.*
import com.tunjid.me.core.utilities.LocalDateTimeSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val FILE_QUERY_LIMIT = 30

val imageMimetypes = setOf("image/jpeg", "image/jpg", "image/png", "image/gif")
val miscMimeTypes = setOf("text/css", "text/javascript")

@Serializable
data class
ArchiveFileQuery(
    val archiveId: ArchiveId,
    val desc: Boolean = true,
    val offset: Int = 0,
    val limit: Int = FILE_QUERY_LIMIT,
    val mimeTypes: Set<String>? = null,
)

@Serializable
data class ArchiveFile(
    @SerialName("_id")
    val id: ArchiveFileId,
    val url: String,
    val mimeType: String,
    val archiveId: ArchiveId,
    @Serializable(LocalDateTimeSerializer::class)
    val created: Instant,
)