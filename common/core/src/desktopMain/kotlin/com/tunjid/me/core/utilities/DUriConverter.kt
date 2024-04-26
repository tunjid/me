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

import io.ktor.utils.io.core.Input
import io.ktor.utils.io.streams.asInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import kotlin.io.path.extension

actual class ActualUriConverter : UriConverter {
    override fun toInput(uri: LocalUri): Input = when (uri) {
        is FileUri -> File(uri.path).inputStream().asInput()
        else -> throw IllegalArgumentException("Unknown URI type")
    }

    override suspend fun name(uri: LocalUri): String = when (uri) {
        is FileUri -> File(uri.path).name
        else -> throw IllegalArgumentException("Unknown URI type")
    }

    override suspend fun mimeType(uri: LocalUri): String = when (uri) {
        is FileUri -> when (val probedMimetype =
            withContext(Dispatchers.IO) {
                Files.probeContentType(uri.file.toPath())
            }) {
            null -> when (uri.file.toPath().extension) {
                "wasm" -> "application/wasm"
                "map" -> "application/json"
                else -> throw IllegalArgumentException("Unknown mimeType for ${uri.path}")
            }

            else -> probedMimetype
        }

        else -> throw IllegalArgumentException("Unknown mimeType for ${uri.path}")
    }

    override suspend fun contentLength(uri: LocalUri): Long? = when (uri) {
        is FileUri -> uri.file.length()
        else -> throw IllegalArgumentException("Unknown URI type")
    }
}

data class FileUri(
    val file: File,
) : LocalUri {
    override val path: String
        get() = file.path

    override val mimetype: String
        get() = Files.probeContentType(file.toPath())
}