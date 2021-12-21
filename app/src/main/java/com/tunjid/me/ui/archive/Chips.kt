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

package com.tunjid.me.ui.archive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Chips(
    name: String? = null,
    color: Color,
    chips: List<String>,
    onClick: ((String) -> Unit)? = null
) {
    Row(
        modifier = Modifier.wrapContentWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (name != null) {
            Text(
                modifier = Modifier.wrapContentWidth(),
                text = name,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        chips.forEach { text ->
            Chip(
                text = text,
                color = color,
                onClick = onClick
            )
        }
    }
}

@Composable
fun Chip(
    text: String,
    color: Color = MaterialTheme.colors.secondary,
    onClick: ((String) -> Unit)? = null
) {
    Button(
        modifier = Modifier
            .wrapContentSize()
            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
        onClick = { onClick?.invoke(text) },
        colors = ButtonDefaults.buttonColors(backgroundColor = color),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
        )
    }
    Spacer(Modifier.width(8.dp))
}
