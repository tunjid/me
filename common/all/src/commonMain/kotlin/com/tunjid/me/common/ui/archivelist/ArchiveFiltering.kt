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

package com.tunjid.me.common.ui.archivelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.model.minus
import com.tunjid.me.core.model.plus
import com.tunjid.me.common.ui.common.ChipAction
import com.tunjid.me.common.ui.common.ChipEditInfo
import com.tunjid.me.common.ui.common.Chips

@Composable
fun ArchiveFilters(
    item: QueryState,
    onChanged: (Action) -> Unit
) {
    val isExpanded = item.expanded
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = 1.dp,
    ) {

        Column(
            modifier = Modifier,
        ) {
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .fillMaxWidth()
                    .clickable { onChanged(Action.ToggleFilter()) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = "Filters",
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                )
                DropDownButton(isExpanded, onChanged)
            }
            AnimatedVisibility(visible = isExpanded) {
                FilterChips(
                    modifier = Modifier
                        .padding(8.dp)
                        .weight(1F),
                    state = item,
                    onChanged = onChanged
                )
            }
        }
    }
}

@Composable
private fun FilterChips(
    modifier: Modifier,
    state: QueryState,
    onChanged: (Action) -> Unit
) {
    Column(
        modifier = modifier
    ) {
        Chips(
            modifier = Modifier.fillMaxWidth(),
            name = "Categories:",
            chips = state.startQuery.contentFilter.categories.map(com.tunjid.me.core.model.Descriptor.Category::value),
            color = MaterialTheme.colors.primaryVariant,
            editInfo = ChipEditInfo(
                currentText = state.categoryText.value,
                onChipChanged = onChipFilterChanged(
                    state = state,
                    reader = QueryState::categoryText,
                    writer = com.tunjid.me.core.model.Descriptor::Category,
                    onChanged = onChanged
                )
            )
        )
        Chips(
            modifier = Modifier.fillMaxWidth(),
            name = "Tags:",
            chips = state.startQuery.contentFilter.tags.map(com.tunjid.me.core.model.Descriptor.Tag::value),
            color = MaterialTheme.colors.secondary,
            editInfo = ChipEditInfo(
                currentText = state.tagText.value,
                onChipChanged = onChipFilterChanged(
                    state = state,
                    reader = QueryState::tagText,
                    writer = com.tunjid.me.core.model.Descriptor::Tag,
                    onChanged = onChanged
                )
            )
        )
    }
}

@Composable
private fun DropDownButton(
    isExpanded: Boolean,
    onChanged: (Action) -> Unit
) {
    val rotation by animateFloatAsState(if (isExpanded) 0f else -90f)
    Button(
        modifier = Modifier
            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
            .rotate(rotation),
        onClick = { onChanged(Action.ToggleFilter()) },
        shape = RoundedCornerShape(40.dp),
        contentPadding = PaddingValues(4.dp),
        content = {
            Icon(imageVector = Filled.ArrowDropDown, contentDescription = "Arrow")
        })
}

private fun onChipFilterChanged(
    state: QueryState,
    reader: (QueryState) -> com.tunjid.me.core.model.Descriptor,
    writer: (String) -> com.tunjid.me.core.model.Descriptor,
    onChanged: (Action) -> Unit
): (ChipAction) -> Unit = {
    when (it) {
        ChipAction.Added -> onChanged(
            Action.Fetch.Reset(
                query = state.startQuery + reader(state),
            )
        )
        is ChipAction.Changed -> onChanged(Action.FilterChanged(writer(it.text)))
        is ChipAction.Removed -> onChanged(
            Action.Fetch.Reset(
                query = state.startQuery - writer(it.text),
            )
        )
    }
}
