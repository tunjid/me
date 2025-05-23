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

package com.tunjid.me.core.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val Icons.Filled.Preview: ImageVector
    get() {
        if (_preview != null) {
            return _preview!!
        }
        _preview = materialIcon(name = "Filled.Preview") {
            materialPath {
                moveTo(19.0f, 3.0f)
                horizontalLineTo(5.0f)
                curveTo(3.89f, 3.0f, 3.0f, 3.9f, 3.0f, 5.0f)
                verticalLineToRelative(14.0f)
                curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(14.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(5.0f)
                curveTo(21.0f, 3.9f, 20.11f, 3.0f, 19.0f, 3.0f)
                close()
                moveTo(19.0f, 19.0f)
                horizontalLineTo(5.0f)
                verticalLineTo(7.0f)
                horizontalLineToRelative(14.0f)
                verticalLineTo(19.0f)
                close()
                moveTo(13.5f, 13.0f)
                curveToRelative(0.0f, 0.83f, -0.67f, 1.5f, -1.5f, 1.5f)
                reflectiveCurveToRelative(-1.5f, -0.67f, -1.5f, -1.5f)
                curveToRelative(0.0f, -0.83f, 0.67f, -1.5f, 1.5f, -1.5f)
                reflectiveCurveTo(13.5f, 12.17f, 13.5f, 13.0f)
                close()
                moveTo(12.0f, 9.0f)
                curveToRelative(-2.73f, 0.0f, -5.06f, 1.66f, -6.0f, 4.0f)
                curveToRelative(0.94f, 2.34f, 3.27f, 4.0f, 6.0f, 4.0f)
                reflectiveCurveToRelative(5.06f, -1.66f, 6.0f, -4.0f)
                curveTo(17.06f, 10.66f, 14.73f, 9.0f, 12.0f, 9.0f)
                close()
                moveTo(12.0f, 15.5f)
                curveToRelative(-1.38f, 0.0f, -2.5f, -1.12f, -2.5f, -2.5f)
                curveToRelative(0.0f, -1.38f, 1.12f, -2.5f, 2.5f, -2.5f)
                curveToRelative(1.38f, 0.0f, 2.5f, 1.12f, 2.5f, 2.5f)
                curveTo(14.5f, 14.38f, 13.38f, 15.5f, 12.0f, 15.5f)
                close()
            }
        }
        return _preview!!
    }

private var _preview: ImageVector? = null