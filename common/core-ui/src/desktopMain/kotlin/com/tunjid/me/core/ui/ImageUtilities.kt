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

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import java.awt.Image
import java.awt.image.BufferedImage
import kotlin.math.max


fun BufferedImage.adjustTo(
    contentScale: ContentScale,
    size: IntSize,
): BufferedImage = when (contentScale) {
    ContentScale.Crop -> downscaleBy(
        scaleFactor(contentScale = contentScale, size = size).largerScale()
    )
        .cropTo(size)

    ContentScale.Fit -> downscaleBy(
        scaleFactor(contentScale = contentScale, size = size).largerScale()
    )

    ContentScale.FillBounds -> downscaleBy(
        scaleFactor(contentScale = contentScale, size = size).largerScale()
    )

    ContentScale.FillHeight -> downscaleBy(
        scaleFactor(contentScale = contentScale, size = size).scaleY
    )

    ContentScale.FillWidth -> downscaleBy(
        scaleFactor(contentScale = contentScale, size = size).scaleX
    )

    else -> downscaleBy(
        scaleFactor(contentScale = contentScale, size = size).largerScale()
    )
}

private fun BufferedImage.downscaleBy(
    ratio: Float,
): BufferedImage {
    if (ratio > 1f) return this
    val downScaledSize = size().toSize().times(ratio)

    return getScaledInstance(
        downScaledSize.width.toInt(),
        downScaledSize.height.toInt(),
        Image.SCALE_DEFAULT
    ).toBufferedImage()
}

private fun BufferedImage.cropTo(dstSize: IntSize): BufferedImage {
    val srcSize = size()
    if (srcSize.width < 1f || srcSize.height < 1f) return this

    val cropSize = srcSize.cropTo(dstSize)

    return getSubimage(
        (srcSize.width - cropSize.width) / 2,
        (srcSize.height - cropSize.height) / 2,
        cropSize.width,
        cropSize.height
    )
}

private fun Image.toBufferedImage(): BufferedImage {
    val resultImage = BufferedImage(
        getWidth(null),
        getHeight(null),
        BufferedImage.TYPE_INT_ARGB
    )

    val graphics2D = resultImage.createGraphics()
    graphics2D.drawImage(this, 0, 0, null)
    graphics2D.dispose()

    return resultImage
}

fun BufferedImage.size() = IntSize(
    width = width,
    height = height
)

private fun IntSize.aspectRatio(): Float = width.toFloat() / height

private fun IntSize.cropTo(
    dstSize: IntSize,
): IntSize {
    val srcSize = this
    val dstAspectRatio = dstSize.aspectRatio()

    return if (dstAspectRatio > 1) {
        var plausibleWidth = srcSize.width
        var plausibleHeight = (plausibleWidth / dstAspectRatio).toInt()
        if (plausibleHeight > srcSize.height) {
            plausibleHeight = srcSize.height
            plausibleWidth = (plausibleHeight * dstAspectRatio).toInt()
        }
        IntSize(plausibleWidth, plausibleHeight)
    } else {
        var plausibleHeight = srcSize.height
        var plausibleWidth = (dstAspectRatio * plausibleHeight).toInt()
        if (plausibleWidth > srcSize.width) {
            plausibleWidth = srcSize.width
            plausibleHeight = (plausibleWidth / dstAspectRatio).toInt()
        }
        IntSize(plausibleWidth, plausibleHeight)
    }
}

private fun BufferedImage.scaleFactor(
    contentScale: ContentScale,
    size: IntSize,
): ScaleFactor = contentScale.computeScaleFactor(
    size().toSize(),
    size.toSize()
)

private fun ScaleFactor.largerScale() = max(scaleX, scaleY)