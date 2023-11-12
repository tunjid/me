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
import androidx.compose.animation.EnterExitState
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
import androidx.compose.ui.zIndex
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.scaffold.backPreviewModifier
import com.tunjid.me.scaffold.nav.NavState
import com.tunjid.me.scaffold.nav.removedRoutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal interface AdaptiveContentHost {

    val state: Adaptive.NavigationState
    fun routeIn(container: Adaptive.Container?): @Composable () -> Unit
}

@Composable
internal fun SavedStateAdaptiveContentHost(
    navState: StateFlow<NavState>,
    uiState: StateFlow<UiState>,
    content: @Composable AdaptiveContentHost.() -> Unit
) {
    LookaheadScope {
        val saveableStateHolder = rememberSaveableStateHolder()
        val adaptiveContentHost = remember(saveableStateHolder) {
            SavedStateAdaptiveContentHost(
                saveableStateHolder = saveableStateHolder
            )
        }

        LaunchedEffect(adaptiveContentHost) {
            navState.adaptiveNavigationState(uiState).collect(
                adaptiveContentHost::updateState
            )
        }

        adaptiveContentHost.content()
        adaptiveContentHost.SavedStateCleanupEffect(navState)
    }
}

@Stable
private class SavedStateAdaptiveContentHost(
    saveableStateHolder: SaveableStateHolder,
) : AdaptiveContentHost,
    SaveableStateHolder by saveableStateHolder {

    override var state by mutableStateOf(Adaptive.NavigationState.Initial)
        private set

    private val slotsToRoutes =
        mutableStateMapOf<Adaptive.Slot?, @Composable () -> Unit>().also { map ->
            map[null] = {}
            Adaptive.Slot.entries.forEach { slot ->
                map[slot] = movableContentOf {
                    val containerState = state.containerStateFor(slot)
                    Render(containerState)
                }
            }
        }

    val idsAnimatingOut = MutableStateFlow(setOf<String>())
    private val keysToSharedElements = mutableStateMapOf<Any, @Composable (Modifier) -> Unit>()

    override fun routeIn(container: Adaptive.Container?): @Composable () -> Unit {
        val slot = container?.let(state::slotFor)
        return slotsToRoutes.getValue(slot)
    }

    /**
     * Updates [state] with [newState] when there are no conflicting routes that are animating
     * out that would cause conflicts with the [SaveableStateHolder] implementation.
     */
    suspend fun updateState(newState: Adaptive.NavigationState) {
        state = when {
            newState.hasConflictingRoutes(idsAnimatingOut.value) -> {
                // Wait for animation to finish before updating state
                idsAnimatingOut.filterNot(newState::hasConflictingRoutes).first()
                newState
            }

            else -> newState
        }
    }

    fun getOrCreateSharedElement(
        key: Any,
        sharedElement: @Composable (Modifier) -> Unit,
    ): @Composable (Modifier) -> Unit = keysToSharedElements.getOrPut(key) {
        createSharedElement(
            key = key,
            sharedElement = sharedElement,
        )
    }

    private fun createSharedElement(
        key: Any,
        sharedElement: @Composable (Modifier) -> Unit,
    ): @Composable (Modifier) -> Unit {
        val sharedElementData = SharedElementData()
        var inCount by mutableIntStateOf(0)

        return movableContentOf { modifier ->
            val updatedElement by rememberUpdatedState(sharedElement)
            updatedElement(
                Modifier
                    .zIndex(20f)
                    .sharedElement(
                        enabled = LocalSharedElementAnimationStatus.current,
                        sharedElementData = sharedElementData,
                    ) then modifier
            )

            DisposableEffect(Unit) {
                ++inCount
                onDispose {
                    if (--inCount <= 0) keysToSharedElements.remove(key)
                }
            }
        }
    }
}

/**
 * Renders [containerState] into is [Adaptive.Container] with scopes that allow for animations
 * and shared elements.
 */
@Composable
private fun SavedStateAdaptiveContentHost.Render(
    containerState: Adaptive.ContainerState,
) {
    val containerTransition = updateTransition(containerState)
    containerTransition.AnimatedContent(
        contentKey = { it.currentRoute?.id },
        transitionSpec = {
            EnterTransition.None togetherWith ExitTransition.None
        }
    ) { targetContainerState ->
        var canAnimateSharedElements by remember { mutableStateOf(true) }
        with(
            AnimatedAdaptiveContentScope(
                containerState = targetContainerState,
                adaptiveContentHost = this@Render,
                animatedContentScope = this
            )
        ) adaptiveContentScope@{
            // Animate if not fully visible or by the effects to run later
            val animationStatus = canAnimateSharedElements
                    || transition.targetState != EnterExitState.Visible
                    || isInPreview

            when (val route = targetContainerState.currentRoute) {
                // TODO: For the transient content container, gracefully animate out instead of
                //  disappearing
                null -> Unit
                else -> Box(
                    modifier = modifierFor(containerState)
                ) {
                    CompositionLocalProvider(
                        LocalAdaptiveContentScope provides this@adaptiveContentScope,
                        LocalSharedElementAnimationStatus provides animationStatus
                    ) {
                        SaveableStateProvider(route.id) {
                            route.content(this@adaptiveContentScope)
                        }
                    }
                }
            }
        }

        // Transitions only run for change adaptations
        LaunchedEffect(transition.isRunning, transition.currentState) {
            // Change transitions can stop animating shared elements when the transition is complete
            canAnimateSharedElements = when {
                transition.isRunning -> true
                else -> when (containerTransition.targetState.adaptation) {
                    is Adaptive.Adaptation.Change -> when (transition.currentState) {
                        EnterExitState.PreEnter,
                        EnterExitState.PostExit -> true

                        EnterExitState.Visible -> false
                    }
                    // No-op on swaps
                    is Adaptive.Adaptation.Swap -> canAnimateSharedElements
                }
            }
        }

        // Add routes ids that are animating out
        LaunchedEffect(transition.isRunning) {
            if (transition.targetState == EnterExitState.PostExit) {
                val routeId = targetContainerState.currentRoute?.id ?: return@LaunchedEffect
                idsAnimatingOut.update { it + routeId }
            }
        }
        // Remove route ids that have animated out
        DisposableEffect(Unit) {
            onDispose {
                val routeId = targetContainerState.currentRoute?.id ?: return@onDispose
                idsAnimatingOut.update { it - routeId }
            }
        }
    }
}

/**
 * Clean up after navigation routes that have been discarded
 */
@Composable
private fun SavedStateAdaptiveContentHost.SavedStateCleanupEffect(
    navState: StateFlow<NavState>,
) {
    val removedRoutesFlow = remember {
        navState
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
    override val containerState: Adaptive.ContainerState,
    val adaptiveContentHost: SavedStateAdaptiveContentHost,
    val animatedContentScope: AnimatedContentScope
) : Adaptive.ContainerScope, AnimatedVisibilityScope by animatedContentScope {

    @Composable
    override fun rememberSharedContent(
        key: Any,
        sharedElement: @Composable (Modifier) -> Unit
    ): @Composable (Modifier) -> Unit {
        val unsharedElement by rememberUpdatedState(sharedElement)

        val currentNavigationState = adaptiveContentHost.state
        // This container state may be animating out. Look up the actual current route
        val currentRouteInContainer = containerState.container?.let(
            currentNavigationState::routeFor
        )
        val isCurrentlyAnimatingIn = currentRouteInContainer?.id == containerState.currentRoute?.id

        // Do not use the shared element if this content is being animated out
        if (!isCurrentlyAnimatingIn) return unsharedElement

        return adaptiveContentHost.getOrCreateSharedElement(key, sharedElement)
    }
}

/**
 * Creates a shared element between composables
 * @param key the key for the shared element
 * @param sharedElement the element to be shared
 */
@Composable
fun rememberSharedContent(
    key: Any,
    sharedElement: @Composable (Modifier) -> Unit
): @Composable (Modifier) -> Unit =
    when (val scope = LocalAdaptiveContentScope.current) {
        null -> throw IllegalArgumentException(
            "This may only be called from an adaptive content scope"
        )

        else -> when (scope.containerState.container) {
            null -> throw IllegalArgumentException(
                "Shared elements may only be used in non null containers"
            )
            // Allow shared elements in the primary or transient primary content only
            Adaptive.Container.Primary,
            Adaptive.Container.TransientPrimary -> when {
                scope.isInPreview -> EmptyElement
                else -> scope.rememberSharedContent(
                    key = key,
                    sharedElement = sharedElement
                )
            }
            // In the secondary container use the element as is
            Adaptive.Container.Secondary -> sharedElement
        }
    }

private val LocalAdaptiveContentScope = staticCompositionLocalOf<Adaptive.ContainerScope?> {
    null
}

private val LocalSharedElementAnimationStatus = staticCompositionLocalOf {
    true
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

private val EmptyElement: @Composable (Modifier) -> Unit = { modifier -> Box(modifier) }

private val Adaptive.ContainerScope.isInPreview: Boolean
    get() = containerState.container == Adaptive.Container.Primary
            && containerState.adaptation == Adaptive.Adaptation.PrimaryToTransient

/**
 * Checks if any of the new routes coming in has any conflicts with those animating out.
 */
private fun Adaptive.NavigationState.hasConflictingRoutes(
    animatingOutIds: Set<String>
) = animatingOutIds.contains(primaryRoute.id)
        || secondaryRoute?.id?.let(animatingOutIds::contains) == true
        || transientPrimaryRoute?.id?.let(animatingOutIds::contains) == true
