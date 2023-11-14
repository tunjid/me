package com.tunjid.me.scaffold.globalui.adaptive

import androidx.compose.animation.AnimatedContent
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

    fun getOrCreateSharedElement(
        key: Any,
        sharedElement: @Composable (Modifier) -> Unit,
    ): @Composable (Modifier) -> Unit
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
            adaptiveNavigationState(navState, uiState).collect(
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

    override fun getOrCreateSharedElement(
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
                        enabled = LocalAdaptiveContentScope.current?.canAnimateSharedElements == true,
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
        val scope = remember {
            AnimatedAdaptiveContentScope(
                containerState = targetContainerState,
                adaptiveContentHost = this@Render,
                animatedContentScope = this
            )
        }
        scope.containerState = targetContainerState
        // Animate if not fully visible or by the effects to run later
        scope.canAnimateSharedElements = scope.canAnimateSharedElements
                || scope.isInPreview
                || transition.targetState != EnterExitState.Visible

        when (val route = targetContainerState.currentRoute) {
            // TODO: For the transient content container, gracefully animate out instead of
            //  disappearing
            null -> Unit
            else -> Box(
                modifier = modifierFor(containerState)
            ) {
                CompositionLocalProvider(
                    LocalAdaptiveContentScope provides scope
                ) {
                    SaveableStateProvider(route.id) {
                        route.content(scope)
                    }
                }
            }
        }

        // Transitions only run for change adaptations
        LaunchedEffect(transition.isRunning, transition.currentState) {
            // Change transitions can stop animating shared elements when the transition is complete
            scope.canAnimateSharedElements = when {
                transition.isRunning -> true
                else -> when (containerTransition.targetState.adaptation) {
                    is Adaptive.Adaptation.Change -> when (transition.currentState) {
                        EnterExitState.PreEnter,
                        EnterExitState.PostExit -> true

                        EnterExitState.Visible -> false
                    }
                    // No-op on swaps
                    is Adaptive.Adaptation.Swap -> scope.canAnimateSharedElements
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
    LaunchedEffect(removedRoutesFlow) {
        removedRoutesFlow.collect { routes ->
            routes.forEach { route ->
                removeState(route.id)
            }
        }
    }
}

/**
 * Checks if any of the new routes coming in has any conflicts with those animating out.
 */
private fun Adaptive.NavigationState.hasConflictingRoutes(
    animatingOutIds: Set<String>
) = animatingOutIds.contains(primaryRoute.id)
        || secondaryRoute?.id?.let(animatingOutIds::contains) == true
        || transientPrimaryRoute?.id?.let(animatingOutIds::contains) == true

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