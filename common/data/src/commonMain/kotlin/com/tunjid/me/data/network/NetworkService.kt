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

import com.tunjid.me.core.model.Archive
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveUpsert
import com.tunjid.me.core.model.ChangeListId
import com.tunjid.me.core.model.ChangeListItem
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.model.SessionRequest
import com.tunjid.me.core.model.User
import com.tunjid.me.data.local.Keys
import com.tunjid.me.data.local.SessionCookieDao
import com.tunjid.me.data.network.models.NetworkMessage
import com.tunjid.me.data.network.models.NetworkResponse
import com.tunjid.me.data.network.models.UpsertResponse
import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.cookies.HttpCookies
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.readText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.core.Input
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

const val ApiUrl = "https://www.tunjid.com"

internal interface NetworkService {
    suspend fun fetchArchives(
        kind: ArchiveKind,
        options: Map<String, String> = mapOf(),
        tags: List<Descriptor.Tag> = listOf(),
        categories: List<Descriptor.Category> = listOf(),
    ): NetworkResponse<List<Archive>>

    suspend fun fetchArchive(
        kind: ArchiveKind,
        id: ArchiveId,
    ): NetworkResponse<Archive>

    suspend fun upsertArchive(
        kind: ArchiveKind,
        upsert: ArchiveUpsert,
    ): NetworkResponse<UpsertResponse>

    suspend fun uploadArchiveHeaderPhoto(
        kind: ArchiveKind,
        id: ArchiveId,
        photo: Input,
    ): NetworkResponse<NetworkMessage>

    suspend fun signIn(
        sessionRequest: SessionRequest
    ): NetworkResponse<User>

    suspend fun session(): NetworkResponse<User>

    suspend fun changeList(
        key: Keys.ChangeList,
        id: ChangeListId? = null
    ): NetworkResponse<List<ChangeListItem>>
}

internal class KtorNetworkService(
    private val json: Json,
    private val baseUrl: String = ApiUrl,
    sessionCookieDao: SessionCookieDao,
) : NetworkService {

    private val client = HttpClient {
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
    ): NetworkResponse<List<Archive>> = json.parseServerErrors {
        client.get("$baseUrl/api/${kind.type}") {
            options.forEach { (key, value) -> parameter(key, value) }
            if (tags.isNotEmpty()) tags.map(Descriptor.Tag::value).forEach {
                parameter("tag", it)
            }
            if (categories.isNotEmpty()) categories.map(Descriptor.Category::value).forEach {
                parameter("category", it)
            }
        }
    }

    override suspend fun fetchArchive(
        kind: ArchiveKind,
        id: ArchiveId,
    ): NetworkResponse<Archive> = json.parseServerErrors {
        client.get("$baseUrl/api/${kind.type}/${id.value}")
    }

    override suspend fun upsertArchive(
        kind: ArchiveKind,
        upsert: ArchiveUpsert,
    ): NetworkResponse<UpsertResponse> = json.parseServerErrors {
        val id = upsert.id
        val requestBuilder: HttpRequestBuilder.() -> Unit = {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            body = upsert
        }
        when (id) {
            null -> client.post("$baseUrl/api/$kind", requestBuilder)
            else -> client.put("$baseUrl/api/$kind/${id.value}", requestBuilder)
        }
    }

    override suspend fun uploadArchiveHeaderPhoto(
        kind: ArchiveKind,
        id: ArchiveId,
        photo: Input
    ): NetworkResponse<NetworkMessage> = json.parseServerErrors {
        client.submitFormWithBinaryData(
            url = "$baseUrl/${kind.type}/${id.value}",
            formData = formData {
                append(
                    key = "photo",
                    value = InputProvider { photo },
                )
            },
        )
    }

    override suspend fun signIn(
        sessionRequest: SessionRequest
    ): NetworkResponse<User> = json.parseServerErrors {
        client.post("$baseUrl/api/sign-in") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            body = sessionRequest
        }
    }

    override suspend fun session(): NetworkResponse<User> = json.parseServerErrors {
        client.get("$baseUrl/api/session")
    }

    override suspend fun changeList(
        key: Keys.ChangeList, id: ChangeListId?
    ): NetworkResponse<List<ChangeListItem>> = json.parseServerErrors {
        client.get("$baseUrl/api/${key.path}/changelist") {
            if (id != null) parameter("after", id.value)
        }
    }
}

private suspend fun <T> Json.parseServerErrors(body: suspend () -> T): NetworkResponse<T> = try {
    NetworkResponse.Success(body())
} catch (exception: Exception) {
    when (exception) {
        is ClientRequestException -> try {
            decodeFromString(exception.response.readText())
        } catch (deserializationException: Exception) {
            NetworkResponse.Error(message = deserializationException.message ?: "Unknown error")
        }
        else -> throw exception
    }
}
