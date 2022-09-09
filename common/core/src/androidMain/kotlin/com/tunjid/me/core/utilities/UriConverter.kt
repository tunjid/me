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

import android.content.ClipData
import io.ktor.utils.io.core.*


actual class UriConverter {
    // TODO Use Android context to get a safe input stream to read from and convert to [Input]
    actual fun toInput(uri: Uri): Input = TODO()
    actual suspend fun name(uri: Uri): String = TODO()
}

data class ClipItemUri(
    val item: ClipData.Item,
    override val mimeType: String?
) : Uri {
    override val path: String
        get() = item.uri?.toString() ?: item.toString()
}