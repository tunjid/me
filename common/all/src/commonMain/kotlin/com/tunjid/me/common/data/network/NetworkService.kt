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

package com.tunjid.me.common.data.network

import com.tunjid.me.common.data.local.SessionCookieDao
import com.tunjid.me.core.model.Archive
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveUpsert
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.model.SessionRequest
import com.tunjid.me.core.model.User
import com.tunjid.me.common.data.network.models.UpsertResponse
import io.ktor.client.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString

const val ApiUrl = "https://www.tunjid.com"

class NetworkService(
    private val baseUrl: String = ApiUrl,
    sessionCookieDao: SessionCookieDao,
) {
    private val client = HttpClient {
        val json = kotlinx.serialization.json.Json {
            explicitNulls = false
            ignoreUnknownKeys = true
        }

        install(JsonFeature) {
            accept(ContentType.Application.Json, ContentType.Text.Html)
            serializer = KotlinxSerializer(json = json)
        }
        install(HttpCookies) {
            storage = SessionCookiesStorage(sessionCookieDao)
        }
        install(SessionCookieInvalidator) {
            this.sessionCookieDao = sessionCookieDao
            this.networkErrorConverter = { json.decodeFromString(it) }
        }
        install(Logging) {
            level = LogLevel.INFO
            logger = object : Logger {
                override fun log(message: String) {
                    println("Logger Ktor => $message")
                }
            }
        }
    }

    suspend fun fetchArchives(
        kind: com.tunjid.me.core.model.ArchiveKind,
        options: Map<String, String> = mapOf(),
        tags: List<com.tunjid.me.core.model.Descriptor.Tag> = listOf(),
        categories: List<com.tunjid.me.core.model.Descriptor.Category> = listOf(),
    ): List<com.tunjid.me.core.model.Archive> = client.get("$baseUrl/api/${kind.type}") {
        options.forEach { (key, value) -> parameter(key, value) }
        if (tags.isNotEmpty()) tags.map(com.tunjid.me.core.model.Descriptor.Tag::value).forEach {
            parameter("tag", it)
        }
        if (categories.isNotEmpty()) categories.map(com.tunjid.me.core.model.Descriptor.Category::value).forEach {
            parameter("category", it)
        }
    }

    suspend fun fetchArchive(
        kind: com.tunjid.me.core.model.ArchiveKind,
        id: com.tunjid.me.core.model.ArchiveId,
    ): com.tunjid.me.core.model.Archive = client.get("$baseUrl/api/${kind.type}/${id.value}")

    suspend fun upsertArchive(
        kind: com.tunjid.me.core.model.ArchiveKind,
        upsert: com.tunjid.me.core.model.ArchiveUpsert,
    ): UpsertResponse {
        val id = upsert.id
        val requestBuilder: HttpRequestBuilder.() -> Unit = {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            body = upsert
        }
        return when (id) {
            null -> client.post("$baseUrl/api/$kind", requestBuilder)
            else -> client.put("$baseUrl/api/$kind/${id.value}", requestBuilder)
        }
    }

    suspend fun signIn(
        sessionRequest: com.tunjid.me.core.model.SessionRequest
    ): com.tunjid.me.core.model.User = client.post("$baseUrl/api/sign-in") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        body = sessionRequest
    }

    suspend fun session(): com.tunjid.me.core.model.User = client.get("$baseUrl/api/session")
}
