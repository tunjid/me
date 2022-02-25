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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

@Composable
actual fun RemoteImagePainter(imageUrl: String?): Painter? {
    val image: ImageBitmap? by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = imageUrl,
    ) {
        value = withContext(Dispatchers.IO) {
            try {
                if (imageUrl != null) urlStream(imageUrl)
                    .buffered()
                    .use(::loadImageBitmap)
                else null
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

private suspend fun urlStream(url: String) = HttpClient().use {
    ByteArrayInputStream(it.get(url))
}