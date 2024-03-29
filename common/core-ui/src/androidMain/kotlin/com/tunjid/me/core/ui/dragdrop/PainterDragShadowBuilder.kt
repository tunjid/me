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

package com.tunjid.me.core.ui.dragdrop

import android.graphics.Canvas
import android.graphics.Point
import android.view.View
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Canvas as ComposeCanvas

internal class PainterDragShadowBuilder(
    private val density: Density,
    private val dragInfo: DragInfo,
) : View.DragShadowBuilder() {

    override fun onProvideShadowMetrics(outShadowSize: Point, outShadowTouchPoint: Point) {
        outShadowSize.set(dragInfo.size.width.toInt(), dragInfo.size.height.toInt())
        outShadowTouchPoint.set(outShadowSize.x / 2, outShadowSize.y / 2)
    }

    override fun onDrawShadow(canvas: Canvas) {
        when (val painter = dragInfo.dragShadowPainter) {
            null -> super.onDrawShadow(canvas)
            else -> CanvasDrawScope().draw(
                density = density,
                size = dragInfo.size,
                layoutDirection = LayoutDirection.Ltr,
                canvas = ComposeCanvas(canvas)
            ) {
                with(painter) {
                    draw(dragInfo.size)
                }
            }
        }

    }
}