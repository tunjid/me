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

package com.tunjid.me.data.network

import com.tunjid.me.data.local.SessionCookieDao
import com.tunjid.me.common.data.network.SessionCookieInvalidator
import com.tunjid.me.common.data.network.SessionCookiesStorage
import com.tunjid.me.common.data.network.models.UpsertResponse
import com.tunjid.me.core.model.*
import io.ktor.client.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString

const val ApiUrl = "https://www.tunjid.com"

internal interface NetworkService {
    suspend fun fetchArchives(
        kind: ArchiveKind,
        options: Map<String, String> = mapOf(),
        tags: List<Descriptor.Tag> = listOf(),
        categories: List<Descriptor.Category> = listOf(),
    ): List<Archive>

    suspend fun fetchArchive(
        kind: ArchiveKind,
        id: ArchiveId,
    ): Archive

    suspend fun upsertArchive(
        kind: ArchiveKind,
        upsert: ArchiveUpsert,
    ): UpsertResponse

    suspend fun signIn(
        sessionRequest: SessionRequest
    ): User

    suspend fun session(): User
}

internal class KtorNetworkService(
    private val baseUrl: String = ApiUrl,
    sessionCookieDao: SessionCookieDao,
) : NetworkService {
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

    override suspend fun fetchArchives(
        kind: ArchiveKind,
        options: Map<String, String>,
        tags: List<Descriptor.Tag>,
        categories: List<Descriptor.Category>,
    ): List<Archive> = client.get("$baseUrl/api/${kind.type}") {
        options.forEach { (key, value) -> parameter(key, value) }
        if (tags.isNotEmpty()) tags.map(Descriptor.Tag::value).forEach {
            parameter("tag", it)
        }
        if (categories.isNotEmpty()) categories.map(Descriptor.Category::value).forEach {
            parameter("category", it)
        }
    }

    override suspend fun fetchArchive(
        kind: ArchiveKind,
        id: ArchiveId,
    ): Archive = client.get("$baseUrl/api/${kind.type}/${id.value}")

    override suspend fun upsertArchive(
        kind: ArchiveKind,
        upsert: ArchiveUpsert,
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

    override suspend fun signIn(
        sessionRequest: SessionRequest
    ): User = client.post("$baseUrl/api/sign-in") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        body = sessionRequest
    }

    override suspend fun session(): User = client.get("$baseUrl/api/session")
}
