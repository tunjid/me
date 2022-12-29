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

package com.tunjid.me.scaffold.globalui.scaffold

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.navRailWidth
import com.tunjid.me.scaffold.globalui.slices.ToolbarItem
import com.tunjid.me.scaffold.globalui.slices.toolbarState
import com.tunjid.me.scaffold.globalui.toolbarSize
import com.tunjid.me.scaffold.lifecycle.mappedCollectAsStateWithLifecycle
import com.tunjid.me.scaffold.nav.NavState
import com.tunjid.me.scaffold.nav.NavStateHolder
import com.tunjid.me.scaffold.nav.Route404
import com.tunjid.me.scaffold.nav.canGoUp
import com.tunjid.mutator.coroutines.asNoOpStateFlowMutator
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.pop

/**
 * Motionally intelligent top toolbar shared amongst nav routes in the app
 */
@Composable
internal fun BoxScope.AppToolbar(
    globalUiStateHolder: GlobalUiStateHolder,
    navStateHolder: NavStateHolder,
) {
    val state by globalUiStateHolder.state.mappedCollectAsStateWithLifecycle(mapper = UiState::toolbarState)
    val windowSizeClass by globalUiStateHolder.state.mappedCollectAsStateWithLifecycle {
        it.windowSizeClass
    }
    val canGoUp by navStateHolder.state.mappedCollectAsStateWithLifecycle { it.mainNav.canGoUp }
    val onUpPressed = remember {
        { navStateHolder.accept { mainNav.pop() } }
    }

    val items = state.items
    val title = state.toolbarTitle
    val alpha: Float by animateFloatAsState(if (state.visible) 1f else 0f)
    val upButtonSize = if (canGoUp) windowSizeClass.toolbarSize() else 0.dp
    val navRailWidth by animateDpAsState(windowSizeClass.navRailWidth())

    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .offset(y = with(LocalDensity.current) { state.statusBarSize.toDp() })
            .height(windowSizeClass.toolbarSize())
            .alpha(alpha)
            .align(Alignment.TopCenter)
            .fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .width(navRailWidth)
                .height(upButtonSize)
        ) {
            UpButton(
                upButtonSize = upButtonSize,
                canGoUp = canGoUp,
                onUpPressed = onUpPressed,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Title(
                modifier = Modifier.weight(1f),
                title = title
            )
            ActionMenu(
                modifier = Modifier.wrapContentSize(),
                items = items,
                globalUiStateHolder = globalUiStateHolder
            )
        }
    }
}

@Composable
private fun UpButton(
    upButtonSize: Dp,
    canGoUp: Boolean,
    onUpPressed: () -> Unit,
) {

    AnimatedVisibility(visible = canGoUp) {
        Button(
            modifier = Modifier
                .size(upButtonSize),
            onClick = onUpPressed,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
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
private fun Title(
    modifier: Modifier = Modifier,
    title: CharSequence
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            modifier = Modifier,
            text = title.toString(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
internal fun ActionMenu(
    modifier: Modifier = Modifier,
    items: List<ToolbarItem>,
    globalUiStateHolder: GlobalUiStateHolder
) {
    val icons = when {
        items.size < 3 -> items
        else -> items.take(2)
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icons.forEach {
            ToolbarIcon(
                item = it,
                globalUiStateHolder = globalUiStateHolder
            )
        }
    }
}

@Composable
private fun ToolbarIcon(
    item: ToolbarItem,
    globalUiStateHolder: GlobalUiStateHolder
) {
    val clicks by globalUiStateHolder.state
        .mappedCollectAsStateWithLifecycle(mapper = UiState::toolbarMenuClickListener)

    when (val vector = item.imageVector) {
        null -> TextButton(
            modifier = Modifier
                .wrapContentSize(align = Alignment.CenterEnd)
                .padding(horizontal = 4.dp),
            onClick = { clicks(item) },
            content = {
                Text(
                    text = item.text,
                    color = MaterialTheme.colorScheme.onPrimary
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
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

//@Preview
@Composable
fun Test() {
    Box {
        AppToolbar(
            globalUiStateHolder = UiState(
                toolbarTitle = "Hi",
                toolbarShows = true
            ).asNoOpStateFlowMutator(),
            navStateHolder = NavState(
                mainNav = MultiStackNav(
                    name = "App",
                    currentIndex = 0,
                    stacks = listOf(
                        StackNav(
                            name = "Preview",
                            routes = listOf(
                                Route404,
                                Route404
                            )
                        )
                    )
                ),
                supportingRoute = null
            ).asNoOpStateFlowMutator(),
        )
    }
}
