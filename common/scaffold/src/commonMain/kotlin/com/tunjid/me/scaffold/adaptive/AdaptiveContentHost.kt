package com.tunjid.me.scaffold.adaptive

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
import com.tunjid.me.scaffold.scaffold.backPreviewModifier
import com.tunjid.me.scaffold.navigation.AdaptiveRoute
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.strings.RouteParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

@Stable
internal interface AdaptiveContentHost {

    val adaptedState: Adaptive.NavigationState

    @Composable
    fun routeIn(container: Adaptive.Container?)

    fun isCurrentlyShared(key: Any): Boolean

    fun <T> createOrUpdateSharedElement(
        key: Any,
        sharedElement: @Composable (T, Modifier) -> Unit,
    ): @Composable (T, Modifier) -> Unit
}

@Composable
internal fun SavedStateAdaptiveContentHost(
    routeParser: RouteParser<AdaptiveRoute>,
    navState: StateFlow<MultiStackNav>,
    uiState: StateFlow<UiState>,
    content: @Composable AdaptiveContentHost.() -> Unit
) {
    LookaheadScope {
        val coroutineScope = rememberCoroutineScope()
        val saveableStateHolder = rememberSaveableStateHolder()
        val adaptiveContentHost = remember(saveableStateHolder) {
            SavedStateAdaptiveContentHost(
                coroutineScope = coroutineScope,
                routeParser = routeParser,
                navStateFlow = navState,
                uiStateFlow = uiState,
                saveableStateHolder = saveableStateHolder
            )
        }

        LaunchedEffect(adaptiveContentHost) {
            adaptiveContentHost.update()
        }

        adaptiveContentHost.content()
        adaptiveContentHost.SavedStateCleanupEffect()
    }
}

@Stable
private class SavedStateAdaptiveContentHost(
    routeParser: RouteParser<AdaptiveRoute>,
    navStateFlow: StateFlow<MultiStackNav>,
    uiStateFlow: StateFlow<UiState>,
    coroutineScope: CoroutineScope,
    saveableStateHolder: SaveableStateHolder,
) : AdaptiveContentHost,
    SaveableStateHolder by saveableStateHolder,
    ActionStateProducer<Action, StateFlow<Adaptive.NavigationState>>
    by coroutineScope.adaptiveNavigationStateMutator(
        routeParser = routeParser,
        navStateFlow = navStateFlow,
        uiStateFlow = uiStateFlow
    ) {

    override var adaptedState by mutableStateOf(Adaptive.NavigationState.Initial)
        private set

    private val slotsToRoutes =
        mutableStateMapOf<Adaptive.Slot?, @Composable () -> Unit>().also { map ->
            map[null] = {}
            Adaptive.Slot.entries.forEach { slot ->
                map[slot] = movableContentOf {
                    Render(slot)
                }
            }
        }

    private val keysToSharedElements = mutableStateMapOf<Any, SharedElementData<*>>()

    @Composable
    override fun routeIn(container: Adaptive.Container?) {
        val slot = container?.let(adaptedState::slotFor)
        slotsToRoutes.getValue(slot).invoke()
    }

    override fun isCurrentlyShared(key: Any): Boolean =
        keysToSharedElements.contains(key)

    override fun <T> createOrUpdateSharedElement(
        key: Any,
        sharedElement: @Composable (T, Modifier) -> Unit,
    ): @Composable (T, Modifier) -> Unit {
        val sharedElementData = keysToSharedElements.getOrPut(key) {
            SharedElementData(
                key = key,
                sharedElement = sharedElement,
                onRemoved = { keysToSharedElements.remove(key) }
            )
        }
        // Can't really guarantee that the caller will use the same key for the right type
        return sharedElementData.moveableSharedElement
    }

    suspend fun update(): Unit = state.collect(::adaptedState::set)
}

/**
 * Renders [slot] into is [Adaptive.Container] with scopes that allow for animations
 * and shared elements.
 */
@Composable
private fun SavedStateAdaptiveContentHost.Render(
    slot: Adaptive.Slot,
) {
    val containerTransition = updateTransition(adaptedState.containerStateFor(slot))
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
        // While technically a backwards write, it stabilizes and ensures the values are
        // correct at first composition
        scope.containerState = targetContainerState
        // Animate if not fully visible or by the effects to run later
        scope.canAnimateSharedElements = scope.canAnimateSharedElements
                || scope.isInPreview
                || transition.targetState != EnterExitState.Visible

        when (val route = targetContainerState.currentRoute) {
            null -> Unit
            else -> Box(
                modifier = modifierFor(targetContainerState)
            ) {
                CompositionLocalProvider(
                    LocalAdaptiveContentScope provides scope
                ) {
                    SaveableStateProvider(route.id) {
                        route.content()
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
private fun SavedStateAdaptiveContentHost.SavedStateCleanupEffect() {
    LaunchedEffect(Unit) {
        val routeIdsInBackStack = state
            .map { it.backStackIds }
            .distinctUntilChanged()

        val routesAnimatedOut = state
            .distinctUntilChangedBy(Adaptive.NavigationState::routeIdsAnimatingOut)
            .scan(emptySet<String>() to emptySet<String>()) { pair, state ->
                pair.copy(first = pair.second, second = state.routeIdsAnimatingOut)
            }
            .map { (old, new) -> old - new }

        combine(
            routeIdsInBackStack,
            routesAnimatedOut,
            ::Pair
        )
            .distinctUntilChanged()
            .collect { (backStackIds, animatedOutRouteIds) ->
                animatedOutRouteIds
                    .filterNot(backStackIds::contains)
                    .forEach(::removeState)
            }
    }
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