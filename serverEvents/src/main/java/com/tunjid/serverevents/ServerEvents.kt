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

package com.tunjid.serverevents

import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

private val serverEvents = listOf(
    Manager.EVENT_OPEN,
    Manager.EVENT_CLOSE,
    Manager.EVENT_RECONNECT,
    Manager.EVENT_RECONNECT_ATTEMPT,
    Manager.EVENT_RECONNECT_FAILED,
)

sealed class ServerEvent {
    abstract val event: String
    abstract val arguments: List<Any>

    sealed class Response : ServerEvent(){
        data class Manager(
            override val event: String,
            override val arguments: List<Any>,
        ) : Response()

        data class Socket(
            override val event: String,
            override val arguments: List<Any>,
        ) : Response()
    }
    data class Request(
        override val event: String,
        override val arguments: List<Any>,
    ) : ServerEvent()
}

fun serverEvents(
    url: String,
    namespace: String,
    reconnectionAttempts: Int = 3,
    events: List<String> = listOf(),
    dispatcher: CoroutineDispatcher,
    onResponse: (ServerEvent.Response) -> ServerEvent.Request?
): Flow<ServerEvent> = channelFlow {
    val socket = buildServerEventSocket(
        url = url,
        namespace = namespace,
        reconnectionTries = reconnectionAttempts
    )

    events.forEach { event ->
        socket.on(event) {
            val response = ServerEvent.Response.Socket(
                event = event,
                arguments = it.toList()
            )
            channel.trySend(response)
            onResponse(response)?.let { request ->
                socket.emit(
                    request.event,
                    *request.arguments.toTypedArray()
                )
            }
        }
    }
    serverEvents.forEach { event ->
        socket.io().on(event) {
            val response = ServerEvent.Response.Manager(
                event = event,
                arguments = it.toList()
            )
            channel.trySend(response)
            onResponse(response)?.let { request ->
                socket.emit(
                    request.event,
                    *request.arguments.toTypedArray()
                )
            }
        }
    }

    socket.connect()
    awaitClose { socket.close() }
}
    .flowOn(dispatcher)


private fun buildServerEventSocket(
    url: String,
    namespace: String,
    reconnectionTries: Int
): Socket = IO.socket(
    url,
    OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(1, TimeUnit.MINUTES)
        .readTimeout(1, TimeUnit.MINUTES)
//            .ignoreAllSSLErrors()
        .build().let { client ->
            IO.setDefaultOkHttpWebSocketFactory(client)
            IO.setDefaultOkHttpCallFactory(client)
            IO.Options().apply {
                secure = true
                forceNew = true
                reconnection = true
                transports = arrayOf(WebSocket.NAME)
                reconnectionAttempts = reconnectionTries
            }
        }
)
    .io()
    .socket(namespace)

//private fun isConnected(socket: Socket?): Boolean = socket != null && socket.connected()

//fun OkHttpClient.Builder.ignoreAllSSLErrors(): OkHttpClient.Builder {
//    val naiveTrustManager = object : X509TrustManager {
//        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
//        override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) = Unit
//        override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) = Unit
//    }
//
//    val insecureSocketFactory = SSLContext.getInstance("TLSv1.2").apply {
//        val trustAllCerts = arrayOf<TrustManager>(naiveTrustManager)
//        init(null, trustAllCerts, SecureRandom())
//    }.socketFactory
//
//    sslSocketFactory(insecureSocketFactory, naiveTrustManager)
//    hostnameVerifier(HostnameVerifier { _, _ -> true })
//    return this
//}