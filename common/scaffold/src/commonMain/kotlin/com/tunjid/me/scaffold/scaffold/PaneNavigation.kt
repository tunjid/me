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

@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.tunjid.me.scaffold.scaffold

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.snap
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.tunjid.composables.accumulatedoffsetnestedscrollconnection.AccumulatedOffsetNestedScrollConnection
import com.tunjid.composables.accumulatedoffsetnestedscrollconnection.rememberAccumulatedOffsetNestedScrollConnection
import com.tunjid.me.scaffold.navigation.AppStack
import org.jetbrains.compose.resources.stringResource


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PaneScaffoldState.PaneNavigationBar(
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = slideInVertically(initialOffsetY = { it }),
    exitTransition: ExitTransition = slideOutVertically(targetOffsetY = { it }),
    onNavItemReselected: () -> Boolean = { false },
) {
    AnimatedVisibility(
        modifier = modifier
            .sharedElement(
                sharedContentState = rememberSharedContentState(NavigationBarSharedElementKey),
                animatedVisibilityScope = this,
                zIndexInOverlay = NavigationSharedElementZIndex,
            ),
        visible = canShowNavigationBar,
        enter = enterTransition,
        exit = exitTransition,
        content = {
            if (canUseMovableNavigationBar) appState.movableNavigationBar(
                Modifier,
                onNavItemReselected,
            )
            else appState.PaneNavigationBar(
                modifier = Modifier,
                onNavItemReselected = onNavItemReselected,
            )
        },
    )
}

@Composable
fun PaneScaffoldState.PaneNavigationRail(
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = slideInHorizontally(initialOffsetX = { -it }),
    exitTransition: ExitTransition = slideOutHorizontally(targetOffsetX = { -it }),
    onNavItemReselected: () -> Boolean = { false },
) {
    AnimatedVisibility(
        modifier = modifier
            .sharedElement(
                sharedContentState = rememberSharedContentState(NavigationRailSharedElementKey),
                animatedVisibilityScope = this,
                zIndexInOverlay = NavigationSharedElementZIndex,
                boundsTransform = NavigationRailBoundsTransform,
            ),
        visible = canShowNavigationRail,
        enter = enterTransition,
        exit = exitTransition,
        content = {
            if (canUseMovableNavigationRail) appState.movableNavigationRail(
                Modifier,
                onNavItemReselected,
            )
            else appState.PaneNavigationRail(
                modifier = Modifier,
                onNavItemReselected = onNavItemReselected,
            )
        }
    )
}


@Composable
internal fun AppState.PaneNavigationBar(
    modifier: Modifier = Modifier,
    onNavItemReselected: () -> Boolean,
) {
    NavigationBar(
        modifier = modifier,
    ) {
        navItems.forEach { item ->
            NavigationBarItem(
                icon = {
                    BadgedBox(
                        badge = {
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
                    onNavItemSelected(item)
                }
            )
        }
    }
}

@Composable
internal fun AppState.PaneNavigationRail(
    modifier: Modifier = Modifier,
    onNavItemReselected: () -> Boolean,
) {
    NavigationRail(
        modifier = modifier,
    ) {
        navItems.forEach { item ->
            NavigationRailItem(
                selected = item.selected,
                icon = {
                    BadgedBox(
                        badge = {
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
                    onNavItemSelected(item)
                }
            )
        }
    }
}


@Composable
fun bottomNavigationNestedScrollConnection(): AccumulatedOffsetNestedScrollConnection {
    val navigationBarHeight by rememberUpdatedState(UiTokens.bottomNavHeight)
    return rememberAccumulatedOffsetNestedScrollConnection(
        invert = true,
        maxOffset = maxOffset@{
            Offset(
                x = 0f,
                y = (navigationBarHeight + UiTokens.bottomNavHeight).toPx()
            )
        },
        minOffset = { Offset.Zero },
    )
}

private data object NavigationBarSharedElementKey
private data object NavigationRailSharedElementKey

private const val NavigationSharedElementZIndex = 2f

private val NavigationRailBoundsTransform = BoundsTransform { _, _ -> snap() }
