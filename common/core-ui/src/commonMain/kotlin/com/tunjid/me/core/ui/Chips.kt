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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
    val requestFocusOnStart: Boolean = false,
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
        var interactedWith by remember { mutableStateOf(false) }
        var hasFocus by remember { mutableStateOf(false) }
        val listState = rememberLazyListState()

        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(hasFocus, chipInfoList) {
            if (hasFocus && chipInfoList.isNotEmpty()) listState.animateScrollToItem(
                index = chipInfoList.lastIndex
            )
        }

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(
                items = chipInfoList,
                key = { "${it.text}-${it.kind}" },
                itemContent = { info ->
                    Chip(
                        modifier = Modifier.animateItemPlacement(),
                        info = info,
                        onClick = click@{
                            if (onClick == null) return@click
                            onClick.invoke(it)
                            interactedWith = true
                        },
                        editInfo = editInfo,
                    )
                }
            )
            if (editInfo != null) item(
                key = "Input"
            ) {
                ChipInputField(
                    modifier = Modifier
                        .animateItemPlacement()
                        .focusRequester(focusRequester = focusRequester),
                    currentText = editInfo.currentText,
                    editInfo.onChipChanged
                ) {
                    hasFocus = it.isFocused
                    if (it.isFocused) interactedWith = true
                }

                LaunchedEffect(focusRequester) {
                    if (editInfo.requestFocusOnStart) focusRequester.requestFocus()
                }
                LaunchedEffect(chipInfoList, interactedWith) {
                    if (interactedWith) focusRequester.requestFocus()
                }
            }
        }
    }
}

@Composable
private fun ChipInputField(
    modifier: Modifier = Modifier,
    currentText: String,
    onChipChanged: (ChipAction) -> Unit,
    onFocusChanged: (FocusState) -> Unit,
) {
    val textStyle = LocalTextStyle.current.merge(
        TextStyle(color = MaterialTheme.colorScheme.onSurface)
    )

    BasicTextField(
        modifier = modifier
            .wrapContentWidth()
            .onFocusChanged(onFocusChanged)
            .onKeyEvent {
                if (it.key != Key.Enter) return@onKeyEvent false

                onChipChanged(ChipAction.Added)
                true
            },
        maxLines = 1,
        value = currentText,
        onValueChange = { onChipChanged(ChipAction.Changed(it)) },
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onAny = { onChipChanged(ChipAction.Added) },
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
    )
}

@Composable
private fun Chip(
    modifier: Modifier = Modifier,
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
    Row(modifier = modifier) {
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
