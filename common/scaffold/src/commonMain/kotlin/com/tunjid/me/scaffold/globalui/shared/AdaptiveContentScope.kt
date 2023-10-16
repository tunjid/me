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

package com.tunjid.me.scaffold.globalui.shared

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import com.tunjid.me.scaffold.globalui.scaffold.backPreviewModifier
import com.tunjid.me.scaffold.nav.Adaptive
import com.tunjid.me.scaffold.nav.NavStateHolder
import com.tunjid.me.scaffold.nav.containerStateFor
import com.tunjid.me.scaffold.nav.removedRoutes
import kotlinx.coroutines.flow.map

private typealias RouteInSlotLookup = (Adaptive.Slot?) -> (@Composable Adaptive.ContainerScope.() -> Unit)

@Composable
internal fun AdaptiveContainerScope(
    navStateHolder: NavStateHolder,
    adaptiveNavigationState: Adaptive.NavigationState,
    content: @Composable Adaptive.ContainerScope.(RouteInSlotLookup) -> Unit
) {
    LookaheadScope {
        val saveableStateHolder = rememberSaveableStateHolder()
        val routeScope = remember(saveableStateHolder) {
            AdaptiveContentScopeWithSavedState(
                lookaheadLayoutScope = this,
                saveableStateHolder = saveableStateHolder
            )
        }
        with(routeScope) {
            content(rememberSlotToRouteComposableLookup(adaptiveNavigationState))
            SavedStateCleanupEffect(navStateHolder)
        }
    }
}

internal class AdaptiveContentScopeWithSavedState internal constructor(
    lookaheadLayoutScope: LookaheadScope,
    saveableStateHolder: SaveableStateHolder,
) : Adaptive.ContainerScope,
    LookaheadScope by lookaheadLayoutScope,
    SaveableStateHolder by saveableStateHolder

@Composable
private fun AdaptiveContentScopeWithSavedState.rememberSlotToRouteComposableLookup(
    adaptiveNavigationState: Adaptive.NavigationState,
): (Adaptive.Slot?) -> (@Composable Adaptive.ContainerScope.() -> Unit) {
    val updatedState by rememberUpdatedState(adaptiveNavigationState)
    return remember {
        val slotsToRoutes = mutableStateMapOf<Adaptive.Slot?, @Composable Adaptive.ContainerScope.() -> Unit>()
        slotsToRoutes[null] = {}
        Adaptive.Slot.entries.forEach { slot ->
            slotsToRoutes[slot] = movableContentWithReceiverOf<Adaptive.ContainerScope> {
                val containerState by remember {
                    derivedStateOf { updatedState.containerStateFor(slot) }
                }
                Render(containerState)
            }
        }
        slotsToRoutes::getValue
    }
}

@Composable
private fun AdaptiveContentScopeWithSavedState.Render(
    containerState: Adaptive.ContainerState,
) {
    updateTransition(containerState).AnimatedContent(
        contentKey = { it.currentRoute?.id },
        transitionSpec = {
            EnterTransition.None togetherWith ExitTransition.None
        }
    ) { targetMetadata ->
        when (val route = targetMetadata.currentRoute) {
            // TODO: For the transient content container, gracefully animate out instead of
            //  disappearing
            null -> Unit
            else -> Box(
                modifier = modifierFor(targetMetadata)
            ) {
                SaveableStateProvider(route.id) {
                    route.Render()
                }
            }
        }
    }
}

@Composable
private fun AnimatedContentScope.modifierFor(
    containerState: Adaptive.ContainerState
) = when (containerState.container) {
    Adaptive.Container.Primary, Adaptive.Container.Secondary -> FillSizeModifier
        .background(color = MaterialTheme.colorScheme.surface)

    Adaptive.Container.TransientPrimary -> FillSizeModifier
        .backPreviewModifier()

    null -> FillSizeModifier
} then when (val currentRoute = containerState.currentRoute) {
    null -> Modifier
    else -> with(currentRoute.transitionsFor(containerState)) {
        Modifier.animateEnterExit(
            enter = enter,
            exit = exit
        )
    }
}

/**
 * Clean up after navigation routes that have been discarded
 */
@Composable
private fun AdaptiveContentScopeWithSavedState.SavedStateCleanupEffect(
    navStateHolder: NavStateHolder,
) {
    val removedRoutesFlow = remember {
        navStateHolder.state
            .map { it.mainNav }
            .removedRoutes()
    }
    LaunchedEffect(Unit) {
        removedRoutesFlow.collect { routes ->
            routes.forEach { route ->
                removeState(route.id)
            }
        }
    }
}

private val FillSizeModifier = Modifier.fillMaxSize()
