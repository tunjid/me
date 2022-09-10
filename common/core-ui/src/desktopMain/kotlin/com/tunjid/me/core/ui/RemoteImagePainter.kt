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

package com.tunjid.me.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.loadImageBitmap
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

@Composable
actual fun RemoteImagePainter(imageUri: String?): Painter? {
    val image: ImageBitmap? by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = imageUri,
    ) {
        value = withContext(Dispatchers.IO) {
            try {
                when {
                    imageUri == null -> null
                    imageUri.startsWith("http") -> imageUri.remoteInputStream()
                    else -> imageUri.fileInputStream()
                }?.toBitMap()
            } catch (e: Exception) {
                // instead of printing to console, you can also write this to log,
                // or show some error placeholder
//                e.printStackTrace()
                null
            }
        }
    }

    return image?.let { BitmapPainter(it) }
}

private suspend fun String.remoteInputStream() = HttpClient().use {
    ByteArrayInputStream(it.get(this).readBytes())
}

private fun String.fileInputStream() = File(this).inputStream()

private fun InputStream.toBitMap() =
    buffered()
        .use(::loadImageBitmap)