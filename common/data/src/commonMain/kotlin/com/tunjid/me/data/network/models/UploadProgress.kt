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

package com.tunjid.me.data.network.models

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.content.PartData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

sealed class TransferStatus<out T> {
    data class Uploading(val progress: Float) : TransferStatus<Nothing>()

    data class Error(val throwable: Throwable) : TransferStatus<Nothing>()

    data class Done<T>(val item: T) : TransferStatus<T>()
}

inline fun <reified T> HttpClient.upload(
    url: String,
    formData: List<PartData>,
): Flow<TransferStatus<T>> =
    channelFlow {
        val result = try {
            TransferStatus.Done(
                submitFormWithBinaryData(
                    url = url,
                    formData = formData,
                ) {
                    onUpload { bytesSentTotal, contentLength ->
                        channel.send((TransferStatus.Uploading(bytesSentTotal.toFloat() / contentLength)))
                    }
                }.body<T>()
            )
        } catch (throwable: Throwable) {
            TransferStatus.Error(throwable)
        }
        channel.send((result))
    }