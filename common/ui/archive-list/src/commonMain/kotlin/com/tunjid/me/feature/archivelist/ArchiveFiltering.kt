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

package com.tunjid.me.feature.archivelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.model.minus
import com.tunjid.me.core.model.plus
import com.tunjid.me.core.ui.ChipAction
import com.tunjid.me.core.ui.ChipEditInfo
import com.tunjid.me.core.ui.Chips
import com.tunjid.me.scaffold.nav.FilterList

@Composable
fun ArchiveFilters(
    item: QueryState,
    onChanged: (Action) -> Unit
) {
    val isExpanded = item.expanded
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {

        Column(
            modifier = Modifier,
        ) {
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .fillMaxWidth()
                    .clickable { onChanged(Action.ToggleFilter()) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                SortButton(onChanged, item)
                Spacer(modifier = Modifier.width(12.dp))
                DescriptorDetailButton(
                    item.currentQuery.contentFilter.categories + item.currentQuery.contentFilter.tags
                )
                Spacer(modifier = Modifier.weight(1f))
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
private fun SortButton(
    onChanged: (Action) -> Unit,
    item: QueryState
) {
    val desc = item.currentQuery.desc
    val rotation by animateFloatAsState(
        targetValue = if (desc) 0f else 180f
    )
    FilledTonalButton(
        modifier = Modifier
            .animateContentSize()
            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
        onClick = {
            onChanged(
                Action.Fetch.QueryChange(
                    query = item.currentQuery.copy(
                        desc = !item.currentQuery.desc,
                        offset = (item.count - item.currentQuery.offset).toInt()
                    )
                )
            )
        },
        contentPadding = PaddingValues(
            vertical = 4.dp,
            horizontal = 8.dp
        ),
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sorted")
                Icon(
                    modifier = Modifier
                        .padding(horizontal = 1.dp)
                        .rotate(rotation),
                    imageVector = Filled.ArrowDropDown,
                    contentDescription = "Arrow"
                )
                Text("by")
                Icon(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .scale(0.8f),
                    imageVector = Filled.DateRange,
                    contentDescription = "Date"
                )
            }
        })
}

@Composable
private inline fun  DescriptorDetailButton(
    descriptors: List<Descriptor>
) {
    Button(
        modifier = Modifier
            .animateContentSize()
            .lilButton(),
        onClick = {},
        shape = RoundedCornerShape(40.dp),
        contentPadding = PaddingValues(
            vertical = 4.dp,
            horizontal = 8.dp
        ),
        content = {
            Text(
                when (descriptors.size) {
                    0 -> "No filters"
                    else -> "${descriptors.size} filters"
                }
            )
        })
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
            chipInfo = state.currentQuery.descriptorChips<Descriptor.Category>(),
            color = MaterialTheme.colorScheme.secondary,
            editInfo = ChipEditInfo(
                currentText = state.categoryText.value,
                onChipChanged = onChipFilterChanged(
                    state = state,
                    reader = QueryState::categoryText,
                    writer = Descriptor::Category,
                    onChanged = onChanged
                )
            )
        )
        Chips(
            modifier = Modifier.fillMaxWidth(),
            name = "Tags:",
            chipInfo = state.currentQuery.descriptorChips<Descriptor.Tag>(),
            color = MaterialTheme.colorScheme.tertiary,
            editInfo = ChipEditInfo(
                currentText = state.tagText.value,
                onChipChanged = onChipFilterChanged(
                    state = state,
                    reader = QueryState::tagText,
                    writer = Descriptor::Tag,
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
    FilledTonalButton(
        modifier = Modifier
            .lilButton()
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
    reader: (QueryState) -> Descriptor,
    writer: (String) -> Descriptor,
    onChanged: (Action) -> Unit
): (ChipAction) -> Unit = {
    when (it) {
        ChipAction.Added -> onChanged(
            Action.Fetch.QueryChange(
                query = state.currentQuery + reader(state),
            )
        )

        is ChipAction.Changed -> onChanged(
            Action.FilterChanged(
                descriptor = writer(it.text)
            )
        )

        is ChipAction.Removed -> onChanged(
            Action.Fetch.QueryChange(
                query = state.currentQuery - writer(it.text),
            )
        )
    }
}

private fun Modifier.lilButton() = defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
