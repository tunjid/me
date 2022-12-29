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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class ChipAction {
    object Added : ChipAction()
    data class Changed(val text: String) : ChipAction()
    data class Removed(val text: String) : ChipAction()
}

data class ChipEditInfo(
    val currentText: String,
    val onChipChanged: (ChipAction) -> Unit,
)

data class ChipInfo(
    val text: String,
    val kind: ChipKind,
)

sealed class ChipKind {
    object Assist : ChipKind()
    data class Filter(val selected: Boolean) : ChipKind()
    data class Input(val selected: Boolean) : ChipKind()
    object Suggestion : ChipKind()
}

@Composable
fun Chips(
    modifier: Modifier = Modifier,
    name: String? = null,
    color: Color,
    chipInfo: List<ChipInfo>,
    onClick: ((String) -> Unit)? = null,
    editInfo: ChipEditInfo? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (name != null) {
            Text(
                modifier = Modifier.wrapContentWidth(),
                text = name,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        ChipRow {
            chipInfo.forEach { info ->
                Chip(
                    info = info,
                    color = color,
                    onClick = onClick,
                    editInfo = editInfo,
                )
            }
            if (editInfo != null) {
                TextField(
                    modifier = Modifier
                        .wrapContentWidth()
                        .defaultMinSize(minWidth = 40.dp, minHeight = 48.dp)
                        .onKeyEvent {
                            if (it.key != Key.Enter) return@onKeyEvent false

                            editInfo.onChipChanged(ChipAction.Added)
                            true
                        },
                    maxLines = 1,
                    value = editInfo.currentText,
                    onValueChange = { editInfo.onChipChanged(ChipAction.Changed(it)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onAny = { editInfo.onChipChanged(ChipAction.Added) },
                    ),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.onSurface,
                    )
                )
            }
        }
    }
}

@Composable
fun Chip(
    info: ChipInfo,
    color: Color = MaterialTheme.colorScheme.tertiary,
    onClick: ((String) -> Unit)? = null,
    editInfo: ChipEditInfo? = null
) {
    val chipLabel = @Composable {
        Text(
            text = info.text,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
    val trailingIcon = @Composable {
        if (editInfo != null) Icon(
            modifier = Modifier
                .scale(0.6f)
                .clickable { editInfo.onChipChanged(ChipAction.Removed(info.text)) },
            imageVector = Icons.Filled.Close,
            contentDescription = "Close"
        )
    }
    Row {
        when (val kind = info.kind) {
            ChipKind.Assist -> AssistChip(
                onClick = { onClick?.invoke(info.text) },
                label = chipLabel,
                trailingIcon = trailingIcon
            )

            is ChipKind.Filter -> FilterChip(
                selected = kind.selected,
                onClick = { onClick?.invoke(info.text) },
                label = chipLabel,
                trailingIcon = trailingIcon
            )

            is ChipKind.Input -> InputChip(
                selected = kind.selected,
                onClick = { onClick?.invoke(info.text) },
                label = chipLabel,
                trailingIcon = trailingIcon

            )

            ChipKind.Suggestion -> SuggestionChip(
                onClick = { onClick?.invoke(info.text) },
                label = chipLabel,
            )
        }
        Spacer(Modifier.width(4.dp))
    }
}

@Composable
expect fun ChipRow(content: @Composable () -> Unit)

//@Preview
@Composable
private fun PreviewStaticChips() {
    Chips(
        color = Color.Blue,
        chipInfo = listOf(
            "abc",
            "def",
            "ghi",
            "jkl",
            "mno",
            "pqr",
            "stu",
            "vwx",
            "yz",
            "123",
            "456",
            "thi is a long one"
        ).map { ChipInfo(text = it, kind = ChipKind.Assist) }
    )
}

//@Preview
@Composable
private fun PreviewEditableChips() {
    Chips(
        color = Color.Blue,
        chipInfo = listOf(
            "abc",
            "def",
            "ghi",
            "jkl",
            "mno",
            "pqr",
            "stu",
            "vwx",
            "yz",
            "123",
            "456",
            "thi is a long one"
        ).map { ChipInfo(text = it, kind = ChipKind.Assist) },
        editInfo = ChipEditInfo(currentText = "Test", onChipChanged = {})
    )
}
