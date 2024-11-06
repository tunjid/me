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
import com.tunjid.me.data.network.models.NetworkErrorCodes
import com.tunjid.me.data.network.models.NetworkResponse
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.observer.ResponseHandler
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.util.AttributeKey

class ErrorInterceptorConfig {
    internal var networkErrorConverter: ((String) -> NetworkResponse.Error)? = null
    internal var sessionEntityQueries: SessionEntityQueries? = null
}

/**
 * Invalidates session cookies that have expired or are otherwise invalid
 */
internal class SessionCookieInvalidator(
    private val networkErrorConverter: ((String) -> NetworkResponse.Error)?,
    private val sessionEntityQueries: SessionEntityQueries?
) {

    companion object : HttpClientPlugin<ErrorInterceptorConfig, SessionCookieInvalidator> {
        override val key: AttributeKey<SessionCookieInvalidator> =
            AttributeKey("ClientNetworkErrorInterceptor")

        override fun prepare(block: ErrorInterceptorConfig.() -> Unit): SessionCookieInvalidator {
            val config = ErrorInterceptorConfig().apply(block)
            return SessionCookieInvalidator(
                networkErrorConverter = config.networkErrorConverter,
                sessionEntityQueries = config.sessionEntityQueries
            )
        }

        override fun install(feature: SessionCookieInvalidator, scope: HttpClient) {
            val observer: ResponseHandler = responseHandler@{ response ->
                val converter = feature.networkErrorConverter ?: return@responseHandler
                val sessionEntityQueries = feature.sessionEntityQueries ?: return@responseHandler

                if (!response.status.isSuccess())
                    try {
                        val responseText = response.bodyAsText()
                        val error = converter(responseText)

                        if (error.errorCode == NetworkErrorCodes.NotLoggedIn) {
                            sessionEntityQueries.updateCookie(cookie = null)
                        }
                    } catch (_: Throwable) {
                    }
            }

            ResponseObserver.install(ResponseObserver(observer), scope)
        }
    }
}