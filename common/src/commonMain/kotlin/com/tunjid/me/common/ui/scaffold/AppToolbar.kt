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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.me.common.app.AppMutator
import com.tunjid.me.common.app.AppState
import com.tunjid.me.common.app.asAppMutator
import com.tunjid.me.common.app.component1
import com.tunjid.me.common.app.component2
import com.tunjid.me.common.globalui.GlobalUiMutator
import com.tunjid.me.common.globalui.ToolbarItem
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.globalui.toolbarState
import com.tunjid.me.common.nav.NavMutator
import com.tunjid.me.common.nav.Route404
import com.tunjid.me.common.nav.canGoUp
import com.tunjid.me.common.ui.utilities.mappedCollectAsState
import com.tunjid.mutator.accept
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.pop

@Composable
internal fun BoxScope.AppToolbar(
    appMutator: AppMutator,
) {
    val (navMutator, globalUiMutator) = appMutator
    val state by globalUiMutator.state.mappedCollectAsState(mapper = UiState::toolbarState)
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
        UpButton(navMutator = navMutator)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
        ) {
            Title(title)
            ActionMenu(items = items, globalUiMutator = globalUiMutator)
        }
    }
}

@Composable
private fun UpButton(
    navMutator: NavMutator,
) {
    val canGoUp by navMutator.state.mappedCollectAsState(mapper = MultiStackNav::canGoUp)

    AnimatedVisibility(visible = canGoUp) {
        Button(
            modifier = Modifier
                .wrapContentSize()
                .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
            onClick = { navMutator.accept { pop() } },
            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
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
internal fun ActionMenu(
    items: List<ToolbarItem>,
    globalUiMutator: GlobalUiMutator
) {
    val icons = when {
        items.size < 3 -> items
        else -> items.take(2)
    }
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icons.forEach {
            ToolbarIcon(
                item = it,
                globalUiMutator = globalUiMutator
            )
        }
    }
}

@Composable
private fun ToolbarIcon(
    item: ToolbarItem,
    globalUiMutator: GlobalUiMutator
) {
    val clicks by globalUiMutator.state
        .mappedCollectAsState(mapper = UiState::toolbarMenuClickListener)

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
    Box {
        AppToolbar(
            appMutator = AppState(
                ui = UiState(
                    toolbarTitle = "Hi",
                    toolbarShows = true
                ),
                nav = MultiStackNav(
                    name = "App",
                    currentIndex = 0,
                    stacks = listOf(StackNav(name = "Preview", routes = listOf(Route404, Route404)))
                )
            ).asAppMutator,
        )
    }
}
