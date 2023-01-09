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
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.URI

import javax.imageio.ImageIO;

/*
An implementation of an aysnc raster painter with an in memory LRU cache and temp disk cache.
*/
@Composable
actual fun asyncRasterPainter(
    imageUri: String?,
    size: IntSize?,
    contentScale: ContentScale,
): Painter? {
    val cache = LocalPainterCache.current
    val cachedPainter = cache[imageUri to size]
    if (cachedPainter != null) return cachedPainter

    val inputStream by produceState<InputStream?>(
        initialValue = null,
        key1 = imageUri,
    ) {
        withContext(Dispatchers.IO) {
            try {
                val imageSource = when {
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

                value = if (imageSource is ImageSource.Remote.Network) {
                    val destination = savedImageFile(uri = imageSource.uri)
                    File(destination.parent).mkdirs()
                    imageSource.inputStream.use { input ->
                        destination.outputStream().use(input::copyTo)
                    }
                    destination.inputStream()
                } else imageSource?.inputStream
            } catch (e: Exception) {
//                e.printStackTrace()
            }
        }
    }

    val painter by produceState<Painter?>(
        initialValue = null,
        key1 = inputStream,
        key2 = size
    ) {
        value = cache.getOrPut(imageUri to size) {
            when (val readImage = inputStream?.buffered()?.let(ImageIO::read)) {
                null -> null
                else -> when (size) {
                    null -> readImage.toPainter()
                    else -> readImage
                        .adjustTo(
                            contentScale = contentScale,
                            size = size
                        )
                        .toPainter()
                }
            }
        }
    }

    return painter
}

private suspend fun String.remoteInputStream() = HttpClient().use {
    ByteArrayInputStream(it.get(this).readBytes())
}

private fun String.fileInputStream() = File(this).inputStream()

private fun savedImageFile(uri: String) =
    File(
        System.getProperty("java.io.tmpdir"),
        URI(uri).path
    )

private val LocalPainterCache: ProvidableCompositionLocal<MutableMap<Pair<String?, IntSize?>, Painter?>> =
    staticCompositionLocalOf {
        object : LinkedHashMap<Key, Painter?>(0, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, Painter?>?): Boolean {
                return size > 20
            }
        }
    }

private typealias Key = Pair<String?, IntSize?>

sealed class ImageSource {
    abstract val inputStream: InputStream

    sealed class Remote : ImageSource() {
        data class Network(
            val uri: String,
            override val inputStream: InputStream,
        ) : Remote()

        data class Cached(
            override val inputStream: InputStream,
        ) : Remote()
    }


    data class Local(
        override val inputStream: InputStream,
    ) : ImageSource()

}