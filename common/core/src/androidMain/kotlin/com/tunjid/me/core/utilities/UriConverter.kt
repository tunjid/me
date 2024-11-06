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
import android.content.Context
import android.provider.OpenableColumns
import io.ktor.utils.io.core.Input
import io.ktor.utils.io.core.use
import io.ktor.utils.io.errors.IOException
import io.ktor.utils.io.streams.asInput
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.Long
import kotlin.NullPointerException
import kotlin.String
import android.net.Uri as AndroidUri


actual class ActualUriConverter(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher,
) : UriConverter {

    @SuppressLint("Recycle")
    override fun toInput(uri: LocalUri): Input =
        when (uri) {
            is ContentUri -> context
                .contentResolver
                .openInputStream(AndroidUri.parse(uri.path))
                ?.asInput()
                ?: throw IOException("Cannot convert URI to binary stream")

            else -> throw IllegalArgumentException("Unknown URI type")
        }

    override suspend fun name(uri: LocalUri): String = withContext(dispatcher) {
        when (uri) {
            is ContentUri -> readColumn(
                uri = uri,
                openableColumns = OpenableColumns.DISPLAY_NAME
            )

            else -> throw IllegalArgumentException("Unknown URI type")
        }
    }

    override suspend fun mimeType(uri: LocalUri): String =
        when (uri) {
            is ContentUri -> uri.mimetype
                ?: throw NullPointerException("Unknown mime type for $uri")

            else -> throw IllegalArgumentException("Unknown URI type")
        }

    override suspend fun contentLength(uri: LocalUri): Long? = withContext(dispatcher) {
        when (uri) {
            is ContentUri -> readColumn(
                uri = uri,
                openableColumns = OpenableColumns.SIZE
            )

            else -> throw IllegalArgumentException("Unknown URI type")
        }
    }

    @SuppressLint("Range")
    private suspend inline fun <reified T> readColumn(uri: LocalUri, openableColumns: String): T =
        withContext(dispatcher) {
            when (uri) {
                is ContentUri -> {
                    val androidUri = AndroidUri.parse(uri.path)
                    if (!androidUri.scheme.equals("content"))
                        throw IllegalArgumentException("Cannot read cursor with ${T::class.qualifiedName}")

                    val cursor = context.contentResolver.query(
                        androidUri,
                        null,
                        null,
                        null,
                        null
                    ) ?: throw NullPointerException("No cursor")

                    cursor.use {
                        if (!it.moveToFirst()) throw NullPointerException("No matching data")
                        when (T::class) {
                            String::class -> it.getString(it.getColumnIndex(openableColumns))
                            Long::class -> it.getLong(it.getColumnIndex(openableColumns))
                            Int::class -> it.getInt(it.getColumnIndex(openableColumns))
                            else -> throw IllegalArgumentException("Cannot read type ${T::class.qualifiedName} from cursor")
                        } as T
                    }
                }

                else -> throw IllegalArgumentException("Unknown URI type")
            }
        }
}

data class ContentUri(
    override val path: String,
    override val mimetype: String,
) : LocalUri

