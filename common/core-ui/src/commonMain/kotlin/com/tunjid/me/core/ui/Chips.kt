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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
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
    val key: Any? = null,
)

sealed class ChipKind {
    data class Assist(
        val tint: Color? = null,
    ) : ChipKind()

    data class Filter(
        val selected: Boolean,
        val tint: Color? = null,
    ) : ChipKind()

    data class Input(
        val selected: Boolean,
        val tint: Color? = null,
    ) : ChipKind()

    data class Suggestion(
        val tint: Color? = null,
    ) : ChipKind()
}

@Composable
fun Chips(
    modifier: Modifier = Modifier,
    name: String? = null,
    chipInfoList: List<ChipInfo>,
    onClick: ((ChipInfo) -> Unit)? = null,
    editInfo: ChipEditInfo? = null,
) {
    Column(
        modifier = modifier.animateContentSize(),
    ) {
        if (name != null) {
            Text(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .wrapContentWidth(),
                text = name,
                fontSize = 14.sp,
            )
        }
        ChipRow {
            chipInfoList.forEach { info ->
                Chip(
                    info = info,
                    onClick = onClick,
                    editInfo = editInfo,
                )
            }
            if (editInfo != null) {
                val textStyle = LocalTextStyle.current.merge(TextStyle(color = MaterialTheme.colorScheme.onSurface))
                BasicTextField(
                    modifier = Modifier
                        .wrapContentWidth()
                        .onKeyEvent {
                            if (it.key != Key.Enter) return@onKeyEvent false

                            editInfo.onChipChanged(ChipAction.Added)
                            true
                        },
                    maxLines = 1,
                    value = editInfo.currentText,
                    onValueChange = { editInfo.onChipChanged(ChipAction.Changed(it)) },
                    textStyle = textStyle,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onAny = { editInfo.onChipChanged(ChipAction.Added) },
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
//                    colors = TextFieldDefaults.textFieldColors(
//                        containerColor = Color.Transparent,
//                        focusedIndicatorColor = Color.Transparent,
//                        unfocusedIndicatorColor = Color.Transparent,
//                        disabledIndicatorColor = Color.Transparent,
//                        cursorColor = MaterialTheme.colorScheme.onSurface,
//                    )
                )
            }
        }
    }
}

@Composable
fun Chip(
    info: ChipInfo,
    onClick: ((ChipInfo) -> Unit)? = null,
    editInfo: ChipEditInfo? = null,
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
            is ChipKind.Assist -> AssistChip(
                shape = MaterialTheme.shapes.small,
                onClick = { onClick?.invoke(info) },
                colors = when (val tint = kind.tint) {
                    null -> AssistChipDefaults.assistChipColors()
                    else -> AssistChipDefaults.assistChipColors(containerColor = tint)
                },
                label = chipLabel,
                trailingIcon = trailingIcon
            )

            is ChipKind.Filter -> FilterChip(
                selected = kind.selected,
                shape = MaterialTheme.shapes.small,
                colors = when (val tint = kind.tint) {
                    null -> FilterChipDefaults.filterChipColors()
                    else -> FilterChipDefaults.filterChipColors(selectedContainerColor = tint)
                },
                onClick = { onClick?.invoke(info) },
                label = chipLabel,
                trailingIcon = trailingIcon
            )

            is ChipKind.Input -> InputChip(
                selected = kind.selected,
                shape = MaterialTheme.shapes.small,
                colors = when (val tint = kind.tint) {
                    null -> InputChipDefaults.inputChipColors()
                    else -> InputChipDefaults.inputChipColors(selectedContainerColor = tint)
                },
                onClick = { onClick?.invoke(info) },
                label = chipLabel,
                trailingIcon = trailingIcon
            )

            is ChipKind.Suggestion -> SuggestionChip(
                shape = MaterialTheme.shapes.small,
                colors = when (val tint = kind.tint) {
                    null -> SuggestionChipDefaults.suggestionChipColors()
                    else -> SuggestionChipDefaults.suggestionChipColors(containerColor = tint)
                },
                onClick = { onClick?.invoke(info) },
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
        chipInfoList = listOf(
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
        ).map { ChipInfo(text = it, kind = ChipKind.Assist()) }
    )
}

//@Preview
@Composable
private fun PreviewEditableChips() {
    Chips(
        chipInfoList = listOf(
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
        ).map { ChipInfo(text = it, kind = ChipKind.Assist()) },
        editInfo = ChipEditInfo(currentText = "Test", onChipChanged = {})
    )
}
