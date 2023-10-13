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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.LocalGlobalUiStateHolder
import com.tunjid.me.scaffold.nav.AdaptiveContainer
import com.tunjid.me.scaffold.nav.AdaptiveContainerSlot
import com.tunjid.me.scaffold.nav.AdaptiveNavigationState
import com.tunjid.me.scaffold.nav.AdaptiveSlotMetadata
import com.tunjid.me.scaffold.nav.MoveKind
import com.tunjid.me.scaffold.nav.NavStateHolder
import com.tunjid.me.scaffold.nav.adaptiveNavigationState
import com.tunjid.me.scaffold.nav.metadataFor
import com.tunjid.me.scaffold.nav.removedRoutes
import com.tunjid.me.scaffold.nav.slotFor
import kotlinx.coroutines.flow.map

/**
 * Root scaffold for the app
 */
@Composable
fun Scaffold(
    modifier: Modifier,
    navStateHolder: NavStateHolder,
    globalUiStateHolder: GlobalUiStateHolder,
) {
    CompositionLocalProvider(
        LocalGlobalUiStateHolder provides globalUiStateHolder,
    ) {
        val uiStateFlow = remember {
            globalUiStateHolder.state
        }
        val adaptiveNavigationStateFlow = remember {
            navStateHolder.state.adaptiveNavigationState(uiStateFlow)
        }
        val adaptiveNavigationState by adaptiveNavigationStateFlow.collectAsState(
            AdaptiveNavigationState.Initial
        )
        val moveKind by remember {
            derivedStateOf { adaptiveNavigationState.moveKind }
        }
        val saveableStateHolder: SaveableStateHolder = rememberSaveableStateHolder()

        val routeLookup = saveableStateHolder.rememberSlotToRouteComposableLookup(
            adaptiveNavigationState = adaptiveNavigationState,
        )

        Surface {
            Box(
                modifier = modifier.fillMaxSize()
            ) {
                AppNavRail(
                    globalUiStateHolder = globalUiStateHolder,
                    navStateHolder = navStateHolder,
                )
                AppToolbar(
                    globalUiStateHolder = globalUiStateHolder,
                    navStateHolder = navStateHolder,
                )
                AppRouteContainer(
                    globalUiStateHolder = globalUiStateHolder,
                    navStateHolder = navStateHolder,
                    moveKind = moveKind,
                    primaryContent = {
                        routeLookup(
                            adaptiveNavigationState.slotFor(AdaptiveContainer.Primary)
                        ).invoke()
                    },
                    secondaryContent = {
                        routeLookup(
                            adaptiveNavigationState.slotFor(AdaptiveContainer.Secondary)
                        ).invoke()
                    },
                    transientPrimaryContent = {
                        routeLookup(
                            adaptiveNavigationState.slotFor(AdaptiveContainer.TransientPrimary)
                        ).invoke()
                    },
                )
                AppFab(
                    globalUiStateHolder = globalUiStateHolder,
                )
                AppBottomNav(
                    globalUiStateHolder = globalUiStateHolder,
                    navStateHolder = navStateHolder,
                )
                AppSnackBar(
                    globalUiStateHolder = globalUiStateHolder,
                )
            }
        }

        SavedStateCleanupEffect(
            saveableStateHolder = saveableStateHolder,
            navStateHolder = navStateHolder
        )
    }
}

@Composable
private fun SaveableStateHolder.rememberSlotToRouteComposableLookup(
    adaptiveNavigationState: AdaptiveNavigationState,
): (AdaptiveContainerSlot?) -> (@Composable () -> Unit) {
    val updatedState by rememberUpdatedState(adaptiveNavigationState)
    return remember {
        val slotsToRoutes = mutableStateMapOf<AdaptiveContainerSlot?, @Composable () -> Unit>()
        slotsToRoutes[null] = {}
        AdaptiveContainerSlot.entries.forEach { slot ->
            slotsToRoutes[slot] = movableContentOf {
                val metadata by remember {
                    derivedStateOf { updatedState.metadataFor(slot) }
                }
                Render(metadata)
            }
        }
        slotsToRoutes::getValue
    }
}

@Composable
private fun SaveableStateHolder.Render(
    metadata: AdaptiveSlotMetadata,
) {
    updateTransition(metadata).AnimatedContent(
        contentKey = { it.route?.id },
        transitionSpec = {
            EnterTransition.None togetherWith ExitTransition.None
        }
    ) { targetMetadata ->
        if (targetMetadata.route == null) return@AnimatedContent
        SaveableStateProvider(targetMetadata.route.id) {
            targetMetadata.route.Render(modifierFor(targetMetadata))
        }
    }
}

@Composable
private fun AnimatedContentScope.modifierFor(metadata: AdaptiveSlotMetadata) =
    if (metadata.container == null || metadata.route == null) FillSizeModifier
    else when (metadata.moveKind) {
        is MoveKind.Swap -> when (metadata.container) {
            AdaptiveContainer.Primary, AdaptiveContainer.Secondary -> FillSizeModifier
                .background(color = MaterialTheme.colorScheme.surface)

            AdaptiveContainer.TransientPrimary -> FillSizeModifier
                .backPreviewModifier()

        }

        MoveKind.Change -> when (metadata.container) {
            AdaptiveContainer.Primary, AdaptiveContainer.Secondary -> FillSizeModifier
                .background(color = MaterialTheme.colorScheme.surface)
                .animateEnterExit(
                    enter = fadeIn(RouteTransitionAnimationSpec),
                    exit = fadeOut(RouteTransitionAnimationSpec)
                )

            AdaptiveContainer.TransientPrimary -> FillSizeModifier
                .backPreviewModifier()
        }
    }

/**
 * Clean up after navigation routes that have been discarded
 */
@Composable
private fun SavedStateCleanupEffect(
    saveableStateHolder: SaveableStateHolder,
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
                saveableStateHolder.removeState(route.id)
            }
        }
    }
}

/**
 * Modifier that offers a way to preview content behind the primary content
 */
internal expect fun Modifier.backPreviewModifier(): Modifier

private val FillSizeModifier = Modifier.fillMaxSize()

private val RouteTransitionAnimationSpec: FiniteAnimationSpec<Float> = tween(
    durationMillis = 700
)