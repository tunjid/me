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
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext


private const val SessionCookieName = "linesman.id"

internal class SessionCookiesStorage(
    private val sessionEntityQueries: SessionEntityQueries,
    private val dispatcher: CoroutineDispatcher,
) : CookiesStorage {

    override suspend fun get(
        requestUrl: Url
    ): List<Cookie> = withContext(dispatcher) {
        listOfNotNull(
            sessionEntityQueries.session()
                .executeAsOneOrNull()
                ?.cookie
                ?.let(::parseServerSetCookieHeader)
        )
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        if (cookie.name == SessionCookieName) sessionEntityQueries.updateCookie(
            cookie = renderSetCookieHeader(cookie)
        )

        println("Session cookie: $cookie")
    }

    override fun close() {}
}
