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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.scaffold.backPreviewModifier
import com.tunjid.me.scaffold.nav.NavState
import com.tunjid.me.scaffold.nav.removedRoutes
import com.tunjid.mutator.ActionStateProducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

internal interface AdaptiveContentHost {

    val adaptedState: Adaptive.NavigationState
    fun routeIn(container: Adaptive.Container?): @Composable () -> Unit

    fun createOrUpdateSharedElement(
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
        val coroutineScope = rememberCoroutineScope()
        val saveableStateHolder = rememberSaveableStateHolder()
        val adaptiveContentHost = remember(saveableStateHolder) {
            SavedStateAdaptiveContentHost(
                coroutineScope = coroutineScope,
                navStateFlow = navState,
                uiStateFlow = uiState,
                saveableStateHolder = saveableStateHolder
            )
        }

        LaunchedEffect(adaptiveContentHost) {
            adaptiveContentHost.state.collect(
                adaptiveContentHost::adaptedState::set
            )
        }

        adaptiveContentHost.content()
        adaptiveContentHost.SavedStateCleanupEffect(navState)
    }
}

@Stable
private class SavedStateAdaptiveContentHost(
    coroutineScope: CoroutineScope,
    navStateFlow: StateFlow<NavState>,
    uiStateFlow: StateFlow<UiState>,
    saveableStateHolder: SaveableStateHolder,
) : AdaptiveContentHost,
    SaveableStateHolder by saveableStateHolder,
    ActionStateProducer<Action, StateFlow<Adaptive.NavigationState>>
    by coroutineScope.adaptiveNavigationStateMutator(
        navStateFlow,
        uiStateFlow
    ) {

    override var adaptedState by mutableStateOf(Adaptive.NavigationState.Initial)

    private val slotsToRoutes =
        mutableStateMapOf<Adaptive.Slot?, @Composable () -> Unit>().also { map ->
            map[null] = {}
            Adaptive.Slot.entries.forEach { slot ->
                map[slot] = movableContentOf {
                    val containerState = adaptedState.containerStateFor(slot)
                    Render(containerState)
                }
            }
        }

    private val keysToSharedElements = mutableStateMapOf<Any, SharedElementData>()

    override fun routeIn(container: Adaptive.Container?): @Composable () -> Unit {
        val slot = container?.let(adaptedState::slotFor)
        return slotsToRoutes.getValue(slot)
    }

    override fun createOrUpdateSharedElement(
        key: Any,
        sharedElement: @Composable (Modifier) -> Unit,
    ): @Composable (Modifier) -> Unit {
        val sharedElementData = keysToSharedElements.getOrPut(key) {
            SharedElementData(sharedElement) { keysToSharedElements.remove(key) }
        }
        return sharedElementData.moveableSharedElement.also {
            if (sharedElementData.currentSharedElement != sharedElement) {
                sharedElementData.currentSharedElement = sharedElement
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
                accept(Action.RouteExitStart(routeId))
            }
        }
        // Remove route ids that have animated out
        DisposableEffect(Unit) {
            onDispose {
                val routeId = targetContainerState.currentRoute?.id ?: return@onDispose
                accept(Action.RouteExitEnd(routeId))
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