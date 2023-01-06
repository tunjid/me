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

package com.tunjid.me.core.utilities

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.provider.OpenableColumns
import io.ktor.utils.io.core.*
import io.ktor.utils.io.errors.*
import io.ktor.utils.io.streams.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import android.net.Uri as AndroidUri


actual class ActualUriConverter(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher,
) : UriConverter {

    @SuppressLint("Recycle")
    override fun toInput(uri: LocalUri): Input =
        when (uri) {
            is ClipItemUri -> context
                .contentResolver
                .openInputStream(AndroidUri.parse(uri.path))
                ?.asInput()
                ?: throw IOException("Cannot convert URI to binary stream")

            else -> throw IllegalArgumentException("Unknown URI type")
        }

    override suspend fun name(uri: LocalUri): String = withContext(dispatcher) {
        when (uri) {
            is ClipItemUri -> {
                val androidUri = AndroidUri.parse(uri.path)
                var result: String? = null
                if (androidUri.scheme.equals("content")) {
                    val cursor = context.contentResolver.query(
                        androidUri,
                        null,
                        null,
                        null,
                        null
                    )
                    cursor?.use {
                        if (it.moveToFirst()) {
                            @SuppressLint("Range")
                            result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        }
                    }
                }
                var finalResult = result
                if (finalResult == null) {
                    finalResult = androidUri.path
                    val cut = finalResult?.lastIndexOf('/')
                    if (finalResult != null && cut != null && cut != -1) {
                        finalResult = finalResult.substring(cut + 1)
                    }
                }
                finalResult ?: "unknown-name"
            }

            else -> throw IllegalArgumentException("Unknown URI type")
        }
    }

    override suspend fun mimeType(uri: LocalUri): String =
        when (uri) {
            is ClipItemUri -> uri.mimeType
                ?: context.contentResolver.getType(AndroidUri.parse(uri.path))
                ?: throw NullPointerException("Unknown mime tipe for $uri")

            else -> throw IllegalArgumentException("Unknown URI type")
        }
}

data class ClipItemUri(
    val item: ClipData.Item,
    val mimeType: String?,
) : LocalUri {
    override val path: String
        get() = item.uri?.toString() ?: item.toString()
}
