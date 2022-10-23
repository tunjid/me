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

import com.tunjid.me.common.data.SessionEntityQueries
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveUpsert
import com.tunjid.me.core.model.ChangeListItem
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.model.SessionRequest
import com.tunjid.me.core.sync.SyncRequest
import com.tunjid.me.data.network.models.NetworkArchive
import com.tunjid.me.data.network.models.NetworkResponse
import com.tunjid.me.data.network.models.NetworkUser
import com.tunjid.me.data.network.models.UpsertResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

const val ApiUrl = "https://www.tunjid.com"

internal interface NetworkService {
    suspend fun fetchArchives(
        kind: ArchiveKind,
        options: Map<String, String> = mapOf(),
        ids: List<ArchiveId>? = null,
        tags: List<Descriptor.Tag> = listOf(),
        categories: List<Descriptor.Category> = listOf(),
    ): NetworkResponse<List<NetworkArchive>>

    suspend fun fetchArchive(
        kind: ArchiveKind,
        id: ArchiveId,
    ): NetworkResponse<NetworkArchive>

    suspend fun upsertArchive(
        kind: ArchiveKind,
        upsert: ArchiveUpsert,
    ): NetworkResponse<UpsertResponse>

    suspend fun uploadArchiveHeaderPhoto(
        kind: ArchiveKind,
        id: ArchiveId,
        name: String,
        mime: String,
        photo: Input,
    ): NetworkResponse<NetworkArchive>

    suspend fun signIn(
        sessionRequest: SessionRequest
    ): NetworkResponse<NetworkUser>

    suspend fun session(): NetworkResponse<NetworkUser>

    suspend fun changeList(
        request: SyncRequest
    ): NetworkResponse<List<ChangeListItem>>
}

internal class KtorNetworkService(
    private val json: Json,
    private val baseUrl: String = ApiUrl,
    sessionEntityQueries: SessionEntityQueries,
    dispatcher: CoroutineDispatcher,
) : NetworkService {

    private val client = HttpClient {
        expectSuccess = true
        install(ContentNegotiation) {
            json(
                json = json,
                contentType = ContentType.Application.Json
            )
        }
        install(HttpCookies) {
            storage = SessionCookiesStorage(
                sessionEntityQueries = sessionEntityQueries,
                dispatcher = dispatcher
            )
        }
        install(SessionCookieInvalidator) {
            this.sessionEntityQueries = sessionEntityQueries
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
        ids: List<ArchiveId>?,
        tags: List<Descriptor.Tag>,
        categories: List<Descriptor.Category>,
    ): NetworkResponse<List<NetworkArchive>> = json.parseServerErrors {
        client.get("$baseUrl/api/${kind.type}") {
            options.forEach { (key, value) -> parameter(key, value) }
            ids?.map(ArchiveId::value)?.forEach {
                parameter("id", it)
            }
            if (tags.isNotEmpty()) tags.map(Descriptor.Tag::value).forEach {
                parameter("tag", it)
            }
            if (categories.isNotEmpty()) categories.map(Descriptor.Category::value).forEach {
                parameter("category", it)
            }
        }.body()
    }

    override suspend fun fetchArchive(
        kind: ArchiveKind,
        id: ArchiveId,
    ): NetworkResponse<NetworkArchive> = json.parseServerErrors {
        client.get("$baseUrl/api/${kind.type}/${id.value}").body()
    }

    override suspend fun upsertArchive(
        kind: ArchiveKind,
        upsert: ArchiveUpsert,
    ): NetworkResponse<UpsertResponse> = json.parseServerErrors {
        val id = upsert.id
        val requestBuilder: HttpRequestBuilder.() -> Unit = {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(upsert)
        }
        when (id) {
            null -> client.post("$baseUrl/api/$kind", requestBuilder)
            else -> client.put("$baseUrl/api/$kind/${id.value}", requestBuilder)
        }.body()
    }

    override suspend fun uploadArchiveHeaderPhoto(
        kind: ArchiveKind,
        id: ArchiveId,
        name: String,
        mime: String,
        photo: Input
    ): NetworkResponse<NetworkArchive> = json.parseServerErrors {
        client.submitFormWithBinaryData(
            url = "$baseUrl/api/${kind.type}/${id.value}",
            formData = formData {
                append(
                    key = "photo",
                    value = InputProvider { photo },
                    headers = buildHeaders {
                        append(HttpHeaders.ContentType, mime)
                        append(HttpHeaders.ContentDisposition, "filename=$name")
                    }
                )
            },
        ) {
            // TODO, make this a flow of upload progress so the UI can display a progress bar
            onUpload { bytesSentTotal, _ ->
                println("Uploaded $bytesSentTotal for header photo")
            }
        }.body()
    }

    override suspend fun signIn(
        sessionRequest: SessionRequest
    ): NetworkResponse<NetworkUser> = json.parseServerErrors {
        client.post("$baseUrl/api/sign-in") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(sessionRequest)
        }.body()
    }

    override suspend fun session(): NetworkResponse<NetworkUser> = json.parseServerErrors {
        client.get("$baseUrl/api/session").body()
    }

    override suspend fun changeList(
        request: SyncRequest
    ): NetworkResponse<List<ChangeListItem>> = json.parseServerErrors {
        client.get("$baseUrl/api/${request.model}/changelist") {
            request.after?.let { changeListItem ->
                parameter("after", changeListItem.changeId.value)
            }
        }.body()
    }
}

private suspend fun <T> Json.parseServerErrors(body: suspend () -> T): NetworkResponse<T> = try {
    NetworkResponse.Success(body())
} catch (exception: Exception) {
    exception.printStackTrace()
    when (exception) {
        is ResponseException -> try {
            decodeFromString(exception.response.bodyAsText())
        } catch (deserializationException: Exception) {
            NetworkResponse.Error(message = exception.response.bodyAsText())
        }

        else -> throw exception
    }
}
