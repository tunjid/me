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

package com.tunjid.me.ui.scaffold

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tunjid.me.LocalAppDependencies
import com.tunjid.me.globalui.ToolbarItem
import com.tunjid.me.globalui.ToolbarState
import com.tunjid.me.ui.mapState
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun BoxScope.AppToolbar(stateFlow: StateFlow<ToolbarState>) {
    val scope = rememberCoroutineScope()
    val state by stateFlow.collectAsState()
    val alpha: Float by animateFloatAsState(if (state.visible) 1f else 0f)

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .offset(y = with(LocalDensity.current) { state.statusBarSize.toDp() })
            .height(56.dp)
            .background(color = MaterialTheme.colors.primary)
            .alpha(alpha)
            .align(Alignment.TopCenter)
            .fillMaxWidth(),
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            text = state.toolbarTitle.toString(),
            color = MaterialTheme.colors.onPrimary
        )
        ActionMenu(
            stateFlow = stateFlow.mapState(
                scope = scope,
                mapper = ToolbarState::items
            )
        )
    }
}

@Composable
internal fun ActionMenu(stateFlow: StateFlow<List<ToolbarItem>>) {
    val flow = remember { stateFlow }
    val items by flow.collectAsState()
    val icons = when {
        items.size < 3 -> items
        else -> items.take(2)
    }

    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icons.forEach { ToolbarIcon(it) }
    }
}

@Composable
fun ToolbarIcon(item: ToolbarItem) {
    val uiStateHolder = LocalAppDependencies.current.globalUiMutator
    val scope = rememberCoroutineScope()

    val clicks by uiStateHolder.state
        .mapState(scope) { it.toolbarMenuClickListener }
        .collectAsState()

    when (val vector = item.imageVector) {
        null -> TextButton(
            modifier = Modifier
                .wrapContentSize(align = Alignment.CenterEnd)
                .padding(horizontal = 4.dp),
            onClick = { clicks(item) },
            content = {
                Text(
                    text = item.text,
                    color = MaterialTheme.colors.onPrimary
                )
            }
        )
        else -> IconButton(
            modifier = Modifier
                .wrapContentSize(align = Alignment.CenterEnd)
                .padding(horizontal = 4.dp),
            onClick = { clicks(item) }
        ) {
            Icon(
                imageVector = vector,
                contentDescription = item.text,
                tint = MaterialTheme.colors.onPrimary
            )
        }
    }
}
