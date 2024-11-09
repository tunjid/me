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

package com.tunjid.me.scaffold.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val com.tunjid.me.core.model.ArchiveKind.icon: ImageVector
    get() = iconMap.getOrPut(this) {
        when (this) {
            com.tunjid.me.core.model.ArchiveKind.Articles -> materialIcon(name = "Filled.Article") {
                materialPath {
                    moveTo(19.0f, 3.0f)
                    lineTo(5.0f, 3.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(14.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(14.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    lineTo(21.0f, 5.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    close()
                    moveTo(14.0f, 17.0f)
                    lineTo(7.0f, 17.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineToRelative(7.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveTo(17.0f, 13.0f)
                    lineTo(7.0f, 13.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineToRelative(10.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveTo(17.0f, 9.0f)
                    lineTo(7.0f, 9.0f)
                    lineTo(7.0f, 7.0f)
                    horizontalLineToRelative(10.0f)
                    verticalLineToRelative(2.0f)
                    close()
                }
            }

            com.tunjid.me.core.model.ArchiveKind.Projects -> materialIcon(name = "Filled.Handyman") {
                materialPath {
                    moveTo(21.67f, 18.17f)
                    lineToRelative(-5.3f, -5.3f)
                    horizontalLineToRelative(-0.99f)
                    lineToRelative(-2.54f, 2.54f)
                    verticalLineToRelative(0.99f)
                    lineToRelative(5.3f, 5.3f)
                    curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                    lineToRelative(2.12f, -2.12f)
                    curveTo(22.06f, 19.2f, 22.06f, 18.56f, 21.67f, 18.17f)
                    close()
                }
                materialPath {
                    moveTo(17.34f, 10.19f)
                    lineToRelative(1.41f, -1.41f)
                    lineToRelative(2.12f, 2.12f)
                    curveToRelative(1.17f, -1.17f, 1.17f, -3.07f, 0.0f, -4.24f)
                    lineToRelative(-3.54f, -3.54f)
                    lineToRelative(-1.41f, 1.41f)
                    verticalLineTo(1.71f)
                    lineTo(15.22f, 1.0f)
                    lineToRelative(-3.54f, 3.54f)
                    lineToRelative(0.71f, 0.71f)
                    horizontalLineToRelative(2.83f)
                    lineToRelative(-1.41f, 1.41f)
                    lineToRelative(1.06f, 1.06f)
                    lineToRelative(-2.89f, 2.89f)
                    lineTo(7.85f, 6.48f)
                    verticalLineTo(5.06f)
                    lineTo(4.83f, 2.04f)
                    lineTo(2.0f, 4.87f)
                    lineToRelative(3.03f, 3.03f)
                    horizontalLineToRelative(1.41f)
                    lineToRelative(4.13f, 4.13f)
                    lineToRelative(-0.85f, 0.85f)
                    horizontalLineTo(7.6f)
                    lineToRelative(-5.3f, 5.3f)
                    curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                    lineToRelative(2.12f, 2.12f)
                    curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                    lineToRelative(5.3f, -5.3f)
                    verticalLineToRelative(-2.12f)
                    lineToRelative(5.15f, -5.15f)
                    lineTo(17.34f, 10.19f)
                    close()
                }
            }

            com.tunjid.me.core.model.ArchiveKind.Talks -> materialIcon(name = "Filled.RecordVoiceOver") {
                materialPath {
                    moveTo(9.0f, 9.0f)
                    moveToRelative(-4.0f, 0.0f)
                    arcToRelative(4.0f, 4.0f, 0.0f, true, true, 8.0f, 0.0f)
                    arcToRelative(4.0f, 4.0f, 0.0f, true, true, -8.0f, 0.0f)
                }
                materialPath {
                    moveTo(9.0f, 15.0f)
                    curveToRelative(-2.67f, 0.0f, -8.0f, 1.34f, -8.0f, 4.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineToRelative(16.0f)
                    verticalLineToRelative(-2.0f)
                    curveToRelative(0.0f, -2.66f, -5.33f, -4.0f, -8.0f, -4.0f)
                    close()
                    moveTo(16.76f, 5.36f)
                    lineToRelative(-1.68f, 1.69f)
                    curveToRelative(0.84f, 1.18f, 0.84f, 2.71f, 0.0f, 3.89f)
                    lineToRelative(1.68f, 1.69f)
                    curveToRelative(2.02f, -2.02f, 2.02f, -5.07f, 0.0f, -7.27f)
                    close()
                    moveTo(20.07f, 2.0f)
                    lineToRelative(-1.63f, 1.63f)
                    curveToRelative(2.77f, 3.02f, 2.77f, 7.56f, 0.0f, 10.74f)
                    lineTo(20.07f, 16.0f)
                    curveToRelative(3.9f, -3.89f, 3.91f, -9.95f, 0.0f, -14.0f)
                    close()
                }
            }
        }
    }

private val iconMap = mutableMapOf<com.tunjid.me.core.model.ArchiveKind, ImageVector>()

val Icons.Filled.Sort: ImageVector
    get() {
        if (_sort != null) {
            return _sort!!
        }
        _sort = materialIcon(name = "Filled.Sort") {
            materialPath {
                moveTo(3.0f, 18.0f)
                horizontalLineToRelative(6.0f)
                verticalLineToRelative(-2.0f)
                lineTo(3.0f, 16.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(3.0f, 6.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(18.0f)
                lineTo(21.0f, 6.0f)
                lineTo(3.0f, 6.0f)
                close()
                moveTo(3.0f, 13.0f)
                horizontalLineToRelative(12.0f)
                verticalLineToRelative(-2.0f)
                lineTo(3.0f, 11.0f)
                verticalLineToRelative(2.0f)
                close()
            }
        }
        return _sort!!
    }

private var _sort: ImageVector? = null

val Icons.Filled.FilterList: ImageVector
    get() {
        if (_filterList != null) {
            return _filterList!!
        }
        _filterList = materialIcon(name = "Filled.FilterList") {
            materialPath {
                moveTo(10.0f, 18.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(3.0f, 6.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(18.0f)
                lineTo(21.0f, 6.0f)
                lineTo(3.0f, 6.0f)
                close()
                moveTo(6.0f, 13.0f)
                horizontalLineToRelative(12.0f)
                verticalLineToRelative(-2.0f)
                lineTo(6.0f, 11.0f)
                verticalLineToRelative(2.0f)
                close()
            }
        }
        return _filterList!!
    }

private var _filterList: ImageVector? = null


val Icons.Filled.ExpandAll: ImageVector
    get() {
        if (_expandAll != null) {
            return _expandAll!!
        }
        _expandAll = ImageVector.Builder(
            name = "vector",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(18.17f, 12f)
                lineTo(15f, 8.83f)
                lineTo(16.41f, 7.41f)
                lineTo(21f, 12f)
                lineTo(16.41f, 16.58f)
                lineTo(15f, 15.17f)
                lineTo(18.17f, 12f)
                moveTo(5.83f, 12f)
                lineTo(9f, 15.17f)
                lineTo(7.59f, 16.59f)
                lineTo(3f, 12f)
                lineTo(7.59f, 7.42f)
                lineTo(9f, 8.83f)
                lineTo(5.83f, 12f)
                close()
            }
        }.build()
        return _expandAll!!
    }

private var _expandAll: ImageVector? = null
