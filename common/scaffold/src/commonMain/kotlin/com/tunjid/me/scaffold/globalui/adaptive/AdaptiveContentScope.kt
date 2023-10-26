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

package com.tunjid.me.scaffold.globalui.adaptive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import com.tunjid.me.scaffold.globalui.scaffold.backPreviewModifier
import com.tunjid.me.scaffold.nav.NavStateHolder
import com.tunjid.me.scaffold.nav.removedRoutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map

internal fun interface AdaptiveRouteLookup {
    fun routeIn(slot: Adaptive.Slot?): @Composable () -> Unit
}

@Composable
internal fun AdaptiveContentHost(
    navStateHolder: NavStateHolder,
    adaptiveNavigationState: Adaptive.NavigationState,
    content: @Composable AdaptiveRouteLookup.() -> Unit
) {
    LookaheadScope {
        val saveableStateHolder = rememberSaveableStateHolder()
        val routeScope = remember(saveableStateHolder) {
            AdaptiveContentHost(
                lookaheadLayoutScope = this,
                saveableStateHolder = saveableStateHolder
            )
        }
        CompositionLocalProvider(
            LocalAdaptiveNavigationState provides adaptiveNavigationState,
        ) {
            with(routeScope) {
                content(rememberSlotToRouteComposableLookup(adaptiveNavigationState))
                SavedStateCleanupEffect(navStateHolder)
            }
        }
    }
}

@Stable
private class AdaptiveContentHost(
    lookaheadLayoutScope: LookaheadScope,
    saveableStateHolder: SaveableStateHolder,
) : LookaheadScope by lookaheadLayoutScope,
    SaveableStateHolder by saveableStateHolder {

    private val sharedElementMap = mutableStateMapOf<Any, @Composable (Modifier) -> Unit>()

    fun getOrCreateSharedElement(
        key: Any,
        sharedElement: @Composable (Modifier) -> Unit,
    ): @Composable (Modifier) -> Unit = sharedElementMap.getOrPut(key) {
        createSharedElement(
            key = key,
            sharedElement = sharedElement,
        )
    }

    private fun createSharedElement(
        key: Any,
        sharedElement: @Composable (Modifier) -> Unit,
    ): @Composable (Modifier) -> Unit {
        val sharedElementData = SharedElementData(lookaheadScope = this)
        var inCount by mutableIntStateOf(0)

        return movableContentOf { modifier ->
            val updatedElement by rememberUpdatedState(sharedElement)
            updatedElement(
                modifier.sharedElement(
                    enabled = LocalSharedElementAnimationStatus.current,
                    sharedElementData = sharedElementData,
                )
            )

            DisposableEffect(Unit) {
                ++inCount
                onDispose {
                    if (--inCount <= 0) sharedElementMap.remove(key)
                }
            }
        }
    }
}

@Composable
private fun AdaptiveContentHost.rememberSlotToRouteComposableLookup(
    adaptiveNavigationState: Adaptive.NavigationState,
): AdaptiveRouteLookup {
    val updatedState by rememberUpdatedState(adaptiveNavigationState)
    return remember {
        val slotsToRoutes = mutableStateMapOf<Adaptive.Slot?, @Composable () -> Unit>()
        slotsToRoutes[null] = {}
        Adaptive.Slot.entries.forEach { slot ->
            slotsToRoutes[slot] = movableContentOf {
                val containerState = updatedState.containerStateFor(slot)
                Render(containerState)
            }
        }
        AdaptiveRouteLookup(slotsToRoutes::getValue)
    }
}

/**
 * Renders [containerState] into is [Adaptive.Container] with scopes that allow for animations
 * and shared elements.
 */
@Composable
private fun AdaptiveContentHost.Render(
    containerState: Adaptive.ContainerState,
) {
    val transition = updateTransition(containerState)
    var canAnimateSharedElements by remember { mutableStateOf(true) }

    transition.AnimatedContent(
        contentKey = { it.currentRoute?.id },
        transitionSpec = {
            EnterTransition.None togetherWith ExitTransition.None
        }
    ) { targetContainerState ->
        with(
            AnimatedAdaptiveContentScope(
                containerState = targetContainerState,
                adaptiveContentHost = this@Render,
                animatedContentScope = this
            )
        ) adaptiveContentScope@{
            when (val route = targetContainerState.currentRoute) {
                // TODO: For the transient content container, gracefully animate out instead of
                //  disappearing
                null -> Unit
                else -> Box(
                    modifier = modifierFor(containerState)
                ) {
                    CompositionLocalProvider(
                        LocalSharedElementAnimationStatus provides canAnimateSharedElements
                    ) {
                        SaveableStateProvider(route.id) {
                            route.content(this@adaptiveContentScope)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(containerState) {
        if (transition.targetState.adaptation is Adaptive.Adaptation.Swap) {
            canAnimateSharedElements = true
            // TODO: This is a heuristic, it assumes animations can run for a certain duration
            //  after a swap change
            delay(700)
            canAnimateSharedElements = false
        }
    }

    // Transitions only run for change adaptations
    LaunchedEffect(transition.isRunning) {
        // Change transitions can stop animating shared elements when the transition is complete
        if (!transition.isRunning && transition.targetState.adaptation is Adaptive.Adaptation.Change) {
            canAnimateSharedElements = false
        }
    }
}

/**
 * Clean up after navigation routes that have been discarded
 */
@Composable
private fun AdaptiveContentHost.SavedStateCleanupEffect(
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

/**
 * An implementation of [Adaptive.ContainerScope] that supports animations and shared elements
 */
@Stable
private class AnimatedAdaptiveContentScope(
    val containerState: Adaptive.ContainerState,
    val adaptiveContentHost: AdaptiveContentHost,
    val animatedContentScope: AnimatedContentScope
) : Adaptive.ContainerScope, AnimatedVisibilityScope by animatedContentScope {
    override val adaptation: Adaptive.Adaptation
        get() = containerState.adaptation

    @Composable
    override fun rememberSharedContent(
        key: Any,
        sharedElement: @Composable (Modifier) -> Unit
    ): @Composable (Modifier) -> Unit {
        val currentNavigationState = LocalAdaptiveNavigationState.current
        // This container state may be animating out. Look up the actual current route
        val currentRouteInContainer = containerState.container?.let(
            currentNavigationState::routeFor
        )
        val isCurrentlyAnimatingIn = currentRouteInContainer?.id == containerState.currentRoute?.id

        // Do not use the shared element if this content is being animated out
        if (!isCurrentlyAnimatingIn) return { modifier ->
            sharedElement(modifier)
        }

        return adaptiveContentHost.getOrCreateSharedElement(key, sharedElement)
    }

    @Composable
    override fun isInPreview(): Boolean =
        LocalAdaptiveNavigationState.current.primaryRoute.id == containerState.currentRoute?.id
                && containerState.adaptation == Adaptive.Adaptation.PrimaryToTransient

}

private val LocalAdaptiveNavigationState = staticCompositionLocalOf {
    Adaptive.NavigationState.Initial
}

private val LocalSharedElementAnimationStatus = staticCompositionLocalOf {
    false
}

@Composable
private fun AnimatedVisibilityScope.modifierFor(
    containerState: Adaptive.ContainerState
) = when (containerState.container) {
    Adaptive.Container.Primary, Adaptive.Container.Secondary -> FillSizeModifier
        .background(color = MaterialTheme.colorScheme.surface)
        .then(
            when (val enterAndExit = containerState.currentRoute?.transitionsFor(containerState)) {
                null -> Modifier
                else -> Modifier.animateEnterExit(
                    enter = enterAndExit.enter,
                    exit = enterAndExit.exit
                )
            }
        )

    Adaptive.Container.TransientPrimary -> FillSizeModifier
        .backPreviewModifier()

    null -> FillSizeModifier
}

private val FillSizeModifier = Modifier.fillMaxSize()
