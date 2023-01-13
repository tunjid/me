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
import javax.imageio.ImageIO


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

suspend fun String?.toInputStream() = withContext(Dispatchers.IO) {
    try {
        toImageSource().toInputStream()
    } catch (e: Exception) {
        null
    }
}


fun InputStream?.toPainter(
    size: IntSize?,
    contentScale: ContentScale,
) = try {
    when (val readImage = this?.buffered()?.let(ImageIO::read)) {
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
} catch (e: Exception) {
    null
}

private suspend fun String?.toImageSource() = when {
    this == null -> null
    startsWith("http") -> when (
        val savedFile = savedImageFile(this).takeIf(File::exists)
    ) {
        null -> ImageSource.Remote.Network(
            uri = this,
            inputStream = remoteInputStream()
        )

        else -> ImageSource.Remote.Cached(
            inputStream = savedFile.inputStream()
        )
    }

    else -> ImageSource.Local(
        inputStream = fileInputStream()
    )
}

private fun ImageSource?.toInputStream() = when (this) {
    is ImageSource.Remote.Network -> {
        val destination = savedImageFile(uri = uri)
        File(destination.parent).mkdirs()
        inputStream.use { input ->
            destination.outputStream().use(input::copyTo)
        }
        destination.inputStream()
    }

    else -> this?.inputStream
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