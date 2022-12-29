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

import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation

@kotlinx.serialization.Serializable
data class FormField(
    val id: String,
    val value: String,
    val transformation: VisualTransformation,
)

@Composable
fun FormField(
    modifier: Modifier = Modifier,
    field: FormField,
    onValueChange: (String) -> Unit
) {
    TextField(
        modifier = modifier,
        value = field.value,
        onValueChange = onValueChange,
        visualTransformation = field.transformation,
        label = { Text(text = field.id) }
    )
}

fun List<FormField>.update(updatedField: FormField) = map { field ->
    if (field.id == updatedField.id) updatedField
    else field
}