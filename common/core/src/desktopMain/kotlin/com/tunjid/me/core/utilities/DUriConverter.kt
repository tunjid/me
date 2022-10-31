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

import io.ktor.utils.io.core.*
import io.ktor.utils.io.streams.*
import java.io.File
import java.nio.file.Files

actual class ActualUriConverter : UriConverter {
    override fun toInput(uri: Uri): Input = when {
        uri.path.startsWith("http") -> TODO("unimplemented")
        else -> File(uri.path).inputStream().asInput()
    }

    override suspend fun name(uri: Uri): String = when {
        uri.path.startsWith("http") -> TODO("unimplemented")
        else -> File(uri.path).name
    }

}

data class FileUri(
    val file: File
) : Uri {
    override val path: String
        get() = file.path
    override val mimeType: String?
        get() = Files.probeContentType(file.toPath())
}