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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun AsyncRasterImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf<IntSize?>(null) }

    val painter = rememberAsyncRasterPainter(
        imageUri = imageUrl,
        contentScale = ContentScale.Crop,
        size = size,
    )
    BoxWithConstraints(
        modifier = modifier
    ) {
        size = maxSize()
        if (painter != null) Image(
            modifier = Modifier.fillMaxSize(),
            painter = painter,
            contentScale = ContentScale.Crop,
            contentDescription = "Thumbnail",
        )
    }
}

@Composable
fun BoxWithConstraintsScope.maxSize(): IntSize? =
    if (maxWidth > 0.dp && maxHeight > 0.dp) with(LocalDensity.current) {
        IntSize(
            width = maxWidth.roundToPx(),
            height = maxHeight.roundToPx()
        )
    }
    else null
