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

import com.tunjid.me.core.model.ArchiveFileId
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveUpsert
import com.tunjid.me.core.model.ChangeListItem
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.model.SessionRequest
import com.tunjid.me.core.model.singular
import com.tunjid.me.core.sync.SyncRequest
import com.tunjid.me.core.utilities.FileDesc
import com.tunjid.me.data.network.models.NetworkArchive
import com.tunjid.me.data.network.models.NetworkArchiveFile
import com.tunjid.me.data.network.models.NetworkMessage
import com.tunjid.me.data.network.models.NetworkResponse
import com.tunjid.me.data.network.models.NetworkUser
import com.tunjid.me.data.network.models.TransferStatus
import com.tunjid.me.data.network.models.UpsertResponse
import com.tunjid.me.data.network.models.upload
import com.tunjid.me.data.repository.SavedState
import com.tunjid.me.data.repository.SavedStateRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.utils.buildHeaders
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject

const val ApiUrl: BaseUrl = "https://www.tunjid.com"

typealias BaseUrl = String

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
        fileDesc: FileDesc,
    ): NetworkResponse<NetworkArchive>

    suspend fun uploadArchiveFile(
        kind: ArchiveKind,
        id: ArchiveId,
        fileDesc: FileDesc,
    ): Flow<TransferStatus<NetworkMessage>>

    suspend fun fetchArchiveFiles(
        kind: ArchiveKind,
        ids: List<ArchiveFileId>? = null,
    ): NetworkResponse<List<NetworkArchiveFile>>

    suspend fun deleteArchiveFile(
        kind: ArchiveKind,
        id: ArchiveFileId,
    ): NetworkResponse<NetworkMessage>

    suspend fun signIn(
        sessionRequest: SessionRequest,
    ): NetworkResponse<SavedState.AuthTokens>

    suspend fun session(): NetworkResponse<NetworkUser>

    suspend fun changeList(
        request: SyncRequest,
    ): NetworkResponse<List<ChangeListItem>>
}

@Inject
internal class KtorNetworkService(
    private val json: Json,
    private val baseUrl: BaseUrl,
    savedStateRepository: SavedStateRepository,
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
                readAuth = {
                    savedStateRepository.savedState.value.auth
                },
                saveAuth = { authCookie ->
                    savedStateRepository.updateState {
                        copy(auth = auth?.copy(auth = authCookie))
                    }
                },
                dispatcher = dispatcher
            )
        }
        install(SessionCookieInvalidator) {
            this.saveAuth = { auth ->
                savedStateRepository.updateState { copy(auth = auth) }
            }
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
        fileDesc: FileDesc,
    ): NetworkResponse<NetworkArchive> = json.parseServerErrors {
        client.submitFormWithBinaryData(
            url = "$baseUrl/api/${kind.type}/${id.value}",
            formData = fileDesc.toFormData("photo"),
        ) {
            // TODO, make this a flow of upload progress so the UI can display a progress bar
            onUpload { bytesSentTotal, _ ->
                println("Uploaded $bytesSentTotal for header photo")
            }
        }.body()
    }

    override suspend fun uploadArchiveFile(
        kind: ArchiveKind,
        id: ArchiveId,
        fileDesc: FileDesc,
    ): Flow<TransferStatus<NetworkMessage>> =
        client.upload(
            url = "$baseUrl/api/${kind.type}/${id.value}/files",
            formData = fileDesc.toFormData("file"),
        )

    override suspend fun fetchArchiveFiles(
        kind: ArchiveKind,
        ids: List<ArchiveFileId>?,
    ): NetworkResponse<List<NetworkArchiveFile>> = json.parseServerErrors {
        client.get("$baseUrl/api/${kind.singular}files") {
            ids?.map(ArchiveFileId::value)?.forEach {
                parameter("id", it)
            }
        }.body()
    }

    override suspend fun deleteArchiveFile(
        kind: ArchiveKind,
        id: ArchiveFileId,
    ): NetworkResponse<NetworkMessage> = json.parseServerErrors {
        client.delete("$baseUrl/api/${kind.type}files") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }.body()
    }

    override suspend fun signIn(
        sessionRequest: SessionRequest,
    ): NetworkResponse<SavedState.AuthTokens> = json.parseServerErrors {
        val response = client.post("$baseUrl/api/sign-in") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(sessionRequest)
        }

        SavedState.AuthTokens(
            authUserId = response.body<NetworkUser>().id,
            auth = response.headers[SessionCookieName]
                ?: throw Exception("No auth token in response")
        )
    }

    override suspend fun session(): NetworkResponse<NetworkUser> = json.parseServerErrors {
        client.get("$baseUrl/api/session").body()
    }

    override suspend fun changeList(
        request: SyncRequest,
    ): NetworkResponse<List<ChangeListItem>> = json.parseServerErrors {
        client.get("$baseUrl/api/${request.model}/changelist") {
            request.after?.let { changeListItem ->
                parameter("after", changeListItem.changeId.value)
            }
        }.body()
    }
}

private suspend inline fun <reified T> Json.parseServerErrors(body: suspend () -> T): NetworkResponse<T> =
    try {
        NetworkResponse.Success(body())
    } catch (exception: Exception) {
        exception.printStackTrace()
        parseErrorResponse(exception)
    }

private suspend fun Json.parseErrorResponse(exception: Exception): NetworkResponse.Error =
    when (exception) {
        is ResponseException -> try {
            decodeFromString(exception.response.bodyAsText())
        } catch (deserializationException: Exception) {
            NetworkResponse.Error(message = exception.response.bodyAsText())
        }

        else -> NetworkResponse.Error(message = exception.message)
    }

private fun FileDesc.toFormData(key: String): List<PartData> = formData {
    append(
        key = key,
        value = InputProvider(size = contentSize) { binaryData },
        headers = buildHeaders {
            append(HttpHeaders.ContentType, mimetype)
            append(HttpHeaders.ContentDisposition, "filename=$name")
        }
    )
}
