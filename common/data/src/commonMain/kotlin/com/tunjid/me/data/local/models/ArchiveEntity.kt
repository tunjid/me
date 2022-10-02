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

package com.tunjid.me.data.local.models

import com.tunjid.me.common.data.ArchiveEntity
import com.tunjid.me.common.data.UserEntity
import com.tunjid.me.core.model.Archive
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.Descriptor
import kotlinx.datetime.Instant

fun ArchiveEntity.toExternalModel(
    author: UserEntity,
    tags: List<String>,
    categories: List<String>
) = Archive(
    id = ArchiveId(id),
    link = link,
    title = title,
    description = description,
    thumbnail = thumbnail,
    videoUrl = videoUrl,
    likes = likes,
    kind = ArchiveKind.values().first { it.type == kind },
    created = Instant.fromEpochMilliseconds(created),
    body = body,
    author = author.toExternalModel(),
    tags = tags.map(Descriptor::Tag),
    categories = categories.map(Descriptor::Category),
)