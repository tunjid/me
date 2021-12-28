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

package com.tunjid.me.common.ui.scaffold

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.me.common.LocalAppDependencies
import com.tunjid.me.common.globalui.ToolbarItem
import com.tunjid.me.common.globalui.ToolbarState
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.nav.MultiStackNav
import com.tunjid.me.common.nav.Route404
import com.tunjid.me.common.nav.StackNav
import com.tunjid.me.common.nav.canGoUp
import com.tunjid.me.common.nav.pop
import com.tunjid.me.common.stubAppDeps
import com.tunjid.me.common.ui.collectAsState
import com.tunjid.mutator.accept
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun BoxScope.AppToolbar(stateFlow: StateFlow<ToolbarState>) {
    val state by stateFlow.collectAsState()
    val items = state.items
    val title = state.toolbarTitle
    val alpha: Float by animateFloatAsState(if (state.visible) 1f else 0f)

    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .offset(y = with(LocalDensity.current) { state.statusBarSize.toDp() })
            .height(56.dp)
            .background(color = MaterialTheme.colors.primary)
            .alpha(alpha)
            .align(Alignment.TopCenter)
            .fillMaxWidth(),
    ) {
        UpButton()
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
        ) {
            Title(title)
            ActionMenu(items = items)
        }
    }
}

@Composable
private fun UpButton() {
    val navMutator = LocalAppDependencies.current.navMutator
    val canGoUp by navMutator.state.collectAsState(mapper = MultiStackNav::canGoUp)

    AnimatedVisibility(visible = canGoUp) {
        Button(
            modifier = Modifier
                .wrapContentSize()
                .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
            onClick = { navMutator.accept { pop() } },
            // Uses ButtonDefaults.ContentPadding by default
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 16.dp,

                )
        ) {
            // Inner content including an icon and a text label
            Icon(
                Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
        }
    }
}

@Composable
private fun Title(title: CharSequence) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            modifier = Modifier,
            text = title.toString(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 18.sp,
            color = MaterialTheme.colors.onPrimary
        )
    }
}

@Composable
internal fun ActionMenu(items: List<ToolbarItem>) {
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

    val clicks by uiStateHolder.state
        .collectAsState(mapper = UiState::toolbarMenuClickListener)

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

//@Preview
@Composable
fun Test() {
    CompositionLocalProvider(
        LocalAppDependencies provides stubAppDeps(
            nav = MultiStackNav(
                currentIndex = 0,
                stacks = listOf(StackNav(name = "Preview", routes = listOf(Route404, Route404)))
            )
        )
    ) {
        Box {
            AppToolbar(
                stateFlow = MutableStateFlow(
                    ToolbarState(
                        statusBarSize = 0,
                        visible = true,
                        overlaps = true,
                        toolbarTitle = "HI",
                        items = listOf(),
                    )
                )
            )
        }
    }
}
