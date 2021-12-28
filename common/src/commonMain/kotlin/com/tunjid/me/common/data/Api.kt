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

package com.tunjid.me.common.data

import com.tunjid.me.common.data.archive.Archive
import com.tunjid.me.common.data.archive.ArchiveKind
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*


class Api(
    private val client: HttpClient,
    private val baseUrl: String = "https://www.tunjid.com"
) {
    suspend fun fetchArchives(
        kind: ArchiveKind,
        options: Map<String, String> = mapOf(),
        tags: List<String> = listOf(),
        categories: List<String> = listOf(),
    ): List<Archive> = client.get("$baseUrl/api/${kind.type}") {
        options.forEach { (key, value) -> parameter(key, value) }
        if (tags.isNotEmpty()) parameter("tag", tags)
        if (categories.isNotEmpty()) parameter("category", categories)

    }

    suspend fun fetchArchive(
        kind: ArchiveKind,
        id: String,
    ): Archive = client.get("$baseUrl/api/${kind.type}/$id")
}
