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

import com.tunjid.me.core.model.Result
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

enum class NetworkErrorCodes(val code: String) {
    NotLoggedIn("not-logged-in"),
    ModelNotFound("model-not-found"),
    RateLimited("rate-limited"),
}

internal sealed class NetworkResponse<T> {
    data class Success<T>(
        val item: T
    ) : NetworkResponse<T>()

    @Serializable
    data class Error<T>(
        @Serializable(NetworkErrorCodesSerializer::class)
        val errorCode: NetworkErrorCodes? = null,
        val message: String? = null,
        val model: String? = null,
    ) : NetworkResponse<T>()
}

internal fun <T> NetworkResponse<T>.item(): T? = when (this) {
    is NetworkResponse.Success -> item
    is NetworkResponse.Error -> null
}

internal fun <T> NetworkResponse<T>.toResult(): Result<T> = when(this) {
    is NetworkResponse.Success -> Result.Success(item)
    is NetworkResponse.Error -> Result.Error(message = message)
}

private object NetworkErrorCodesSerializer : KSerializer<NetworkErrorCodes> {
    override val descriptor = PrimitiveSerialDescriptor("code", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: NetworkErrorCodes) {
        encoder.encodeString(value.code)
    }

    override fun deserialize(decoder: Decoder): NetworkErrorCodes = when (decoder.decodeString()) {
        NetworkErrorCodes.NotLoggedIn.code -> NetworkErrorCodes.NotLoggedIn
        NetworkErrorCodes.ModelNotFound.code -> NetworkErrorCodes.ModelNotFound
        NetworkErrorCodes.RateLimited.code -> NetworkErrorCodes.RateLimited
        else -> throw IllegalArgumentException("Unknown kind")
    }
}