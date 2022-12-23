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
import androidx.compose.runtime.staticCompositionLocalOf
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
import java.net.URI


@Composable
actual fun RemoteImagePainter(imageUri: String?): Painter? {
    val cache = LocalBitmapCache.current
    val cachedPainter = imageUri?.let(cache::get)?.let(::BitmapPainter)
    if (cachedPainter != null) return cachedPainter

    val image: ImageBitmap? by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = imageUri,
    ) {
        val imageSource = withContext(Dispatchers.IO) {
            try {
                when {
                    imageUri == null -> null
                    imageUri.startsWith("http") -> {
                        val savedFile = savedImageFile(imageUri)
                        if (!savedFile.exists()) ImageSource.Remote.Network(
                            uri = imageUri,
                            inputStream = imageUri.remoteInputStream()
                        )
                        else ImageSource.Remote.Cached(
                            savedFile.inputStream()
                        )
                    }

                    else -> ImageSource.Local(
                        imageUri.fileInputStream()
                    )
                }
            } catch (e: Exception) {
//                e.printStackTrace()
                null
            }
        }

        val inputStream = if (imageSource is ImageSource.Remote.Network) {
            val destination = savedImageFile(uri = imageSource.uri)
            File(destination.parent).mkdirs()
            imageSource.inputStream.use { input ->
                destination.outputStream().use(input::copyTo)
            }
            destination.inputStream()
        } else imageSource?.inputStream

        value = inputStream?.toBitMap()
    }

    return image?.let {
        if (imageUri != null) cache[imageUri] = it
        BitmapPainter(it)
    }
}

private suspend fun String.remoteInputStream() = HttpClient().use {
    ByteArrayInputStream(it.get(this).readBytes())
}

private fun String.fileInputStream() = File(this).inputStream()

private fun InputStream.toBitMap() =
    buffered()
        .use(::loadImageBitmap)

private fun savedImageFile(uri: String) =
    File(
        System.getProperty("java.io.tmpdir"),
        URI(uri).path
    )

private val LocalBitmapCache = staticCompositionLocalOf {
    mutableMapOf<String, ImageBitmap>()
}

sealed class ImageSource {
    abstract val inputStream: InputStream

    sealed class Remote : ImageSource() {
        data class Network(
            val uri: String,
            override val inputStream: InputStream
        ) : Remote()

        data class Cached(
            override val inputStream: InputStream
        ) : Remote()
    }


    data class Local(
        override val inputStream: InputStream
    ) : ImageSource()

}