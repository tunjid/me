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

import com.tunjid.me.data.local.SessionCookieDao
import io.ktor.client.features.cookies.*
import io.ktor.http.*
import kotlinx.coroutines.flow.firstOrNull


private const val SessionCookieName = "linesman.id"

internal class SessionCookiesStorage(
    private val sessionCookieDao: SessionCookieDao
) : CookiesStorage {

    override suspend fun get(requestUrl: Url): List<Cookie> = listOfNotNull(
        sessionCookieDao.sessionCookieStream
            .firstOrNull()
            ?.let(::parseServerSetCookieHeader)
    )

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        if (cookie.name == SessionCookieName) sessionCookieDao.saveSessionCookie(
            sessionCookie = renderSetCookieHeader(cookie)
        )

        println("Session cookie: $cookie")
    }

    override fun close() {}
}
