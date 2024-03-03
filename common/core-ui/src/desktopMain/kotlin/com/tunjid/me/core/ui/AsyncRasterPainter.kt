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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

private enum class SizeBucket {
    Small, Medium, Large;
}

private data class Key(
    val imageUri: String?,
    val contentScale: ContentScale,
    val sizeBucket: SizeBucket,
)

@Composable
actual fun AsyncRasterImage(
    args: MediaArgs,
    modifier: Modifier,
) {
    var size by remember { mutableStateOf<IntSize?>(null) }
    val imageModifier = modifier.onSizeChanged { size = it }

    val painter = rememberAsyncRasterPainter(
        imageUri = args.url,
        contentScale = args.contentScale,
        size = size,
    )
    if (painter != null) Image(
        modifier = imageModifier,
        painter = painter,
        contentScale = args.contentScale,
        contentDescription = args.description,
    )
    else Box(imageModifier)
}

/*
An implementation of an aysnc raster painter with an in memory LRU cache and temp disk cache.
*/
@Composable
private fun rememberAsyncRasterPainter(
    imageUri: String?,
    size: IntSize?,
    contentScale: ContentScale,
): Painter? {
    val cache = LocalPainterCache.current
    val cachedPainter = cache[
        Key(
            imageUri = imageUri,
            contentScale = contentScale,
            sizeBucket = size.toBucket()
        )
    ]
    if (cachedPainter != null) return cachedPainter

    val inputStream by produceState<InputStream?>(
        initialValue = null,
        key1 = imageUri,
    ) {
        value = imageUri.toInputStream()
    }

    val painter by produceState<Painter?>(
        initialValue = null,
        key1 = inputStream,
        key2 = contentScale,
        key3 = size
    ) {
        // Do image manipulation on an IO thread
        val painter = withContext(Dispatchers.IO) {
            inputStream.toPainter(size, contentScale)
        }
        // Write to value on the main thread
        withContext(Dispatchers.Main) {
            if (painter != null) value = cache.getOrPut(
                Key(
                    imageUri = imageUri,
                    contentScale = contentScale,
                    sizeBucket = size.toBucket()
                )
            ) {
                painter
            }
        }
    }

    // Use the painter created or the smallest existing one
    return painter ?: cache.entries
        .filter { it.key.imageUri == imageUri }
        .minByOrNull { it.key.sizeBucket }
        ?.value
}

private fun IntSize?.area() = if (this == null) Int.MAX_VALUE else width * height


private val LocalPainterCache: ProvidableCompositionLocal<MutableMap<Key, Painter?>> =
    staticCompositionLocalOf {
        object : LinkedHashMap<Key, Painter?>(0, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, Painter?>?): Boolean {
                return size > 20
            }
        }
    }

private fun IntSize?.toBucket() =
    if (this == null) SizeBucket.Large
    else when (area()) {
        in 0..352 * 240 -> SizeBucket.Small
        in 0..640 * 480 -> SizeBucket.Medium
        else -> SizeBucket.Large
    }