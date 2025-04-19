/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.me.scaffold.scaffold

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.me.scaffold.navigation.AppStack
import org.jetbrains.compose.resources.stringResource


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PaneScaffoldState.PaneBottomAppBar(
    modifier: Modifier = Modifier,
    onNavItemReselected: () -> Boolean = { false },
    badge: @Composable (AppStack) -> Unit = {},
) {
    val appState = LocalAppState.current
    val sharedContentState = rememberSharedContentState(BottomNavSharedElementKey)
    NavigationBar(
        modifier = modifier
            .sharedElement(
                sharedContentState = sharedContentState,
                animatedVisibilityScope = this,
                zIndexInOverlay = BottomNavSharedElementZIndex,
            ),
    ) {
        appState.navItems.forEach { item ->
            NavigationBarItem(
                icon = {
                    BadgedBox(
                        badge = {
                            badge(item.stack)
                        },
                        content = {
                            Icon(
                                imageVector = item.stack.icon,
                                contentDescription = stringResource(item.stack.titleRes),
                            )
                        },
                    )
                },
                selected = item.selected,
                onClick = {
                    if (item.selected && onNavItemReselected()) return@NavigationBarItem
                    appState.onNavItemSelected(item)
                }
            )
        }
    }
}

@Suppress("UnusedReceiverParameter")
@Composable
fun PaneScaffoldState.PaneNavigationRail(
    modifier: Modifier = Modifier,
    onNavItemReselected: () -> Boolean = { false },
    badge: @Composable (AppStack) -> Unit = {},
) {
    val appState = LocalAppState.current
    NavigationRail(
        modifier = modifier,
    ) {
        appState.navItems.forEach { item ->
            NavigationRailItem(
                selected = item.selected,
                icon = {
                    BadgedBox(
                        badge = {
                            badge(item.stack)
                        },
                        content = {
                            Icon(
                                imageVector = item.stack.icon,
                                contentDescription = stringResource(item.stack.titleRes),
                            )
                        },
                    )
                },
                onClick = {
                    if (item.selected && onNavItemReselected()) return@NavigationRailItem
                    appState.onNavItemSelected(item)
                }
            )
        }
    }
}

private data object BottomNavSharedElementKey
