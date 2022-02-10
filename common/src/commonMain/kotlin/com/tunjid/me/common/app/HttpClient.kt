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

package com.tunjid.me.common.app

import com.tunjid.me.common.data.local.SessionCookieDao
import io.ktor.client.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

fun httpClient(sessionCookieDao: SessionCookieDao) = HttpClient {
    install(JsonFeature) {
        accept(ContentType.Application.Json, ContentType.Text.Html)
        serializer = KotlinxSerializer(
            json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        )
    }
    install(HttpCookies) {
        storage = SessionCookiesStorage(sessionCookieDao)
    }
    install(Logging) {
        level = LogLevel.INFO
        logger = object : Logger {
            override fun log(message: String) {
//                println("Logger Ktor => $message")
            }
        }
    }
}

private class SessionCookiesStorage(
    sessionCookieDao: SessionCookieDao
) : CookiesStorage {

    override suspend fun get(requestUrl: Url): List<Cookie> = listOf()

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        println("Cookie name: ${cookie.name}")
    }

    override fun close() {}
}