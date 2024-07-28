package com.tunjid.me.scaffold.scaffold

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tunjid.me.scaffold.adaptive.AdaptiveContentState
import com.tunjid.me.scaffold.adaptive.MovableSharedElementData
import com.tunjid.me.scaffold.di.AdaptiveRouter
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.WindowSizeClass
import com.tunjid.scaffold.adaptive.Adaptive
import com.tunjid.scaffold.adaptive.AnimatedAdaptiveContentScope
import com.tunjid.scaffold.adaptive.LocalAdaptiveContentScope
import com.tunjid.treenav.MultiStackNav
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Stable
@Inject
class SavedStateAdaptiveContentState(
    val adaptiveRouter: AdaptiveRouter,
    navStateFlow: StateFlow<MultiStackNav>,
    uiStateFlow: StateFlow<UiState>,
    @Assisted coroutineScope: CoroutineScope,
    @Assisted saveableStateHolder: SaveableStateHolder,
) : AdaptiveContentState,
    SaveableStateHolder by saveableStateHolder {

    private val mutator = coroutineScope.adaptiveNavigationStateMutator(
        adaptiveRouter = adaptiveRouter,
        navStateFlow = navStateFlow,
        uiStateFlow = uiStateFlow,
        onChanged = ::navigationState::set
    )

    internal fun onAction(action: Action) = mutator.accept(action)
    override var navigationState: Adaptive.NavigationState by mutableStateOf(
        value = mutator.state.value
    )
        private set

    private val slotsToRoutes =
        mutableStateMapOf<Adaptive.Slot?, @Composable () -> Unit>().also { map ->
            map[null] = {}
            Adaptive.Container.slots.forEach { slot ->
                map[slot] = movableContentOf {
                    Render(slot)
                }
            }
        }

    private val keysToSharedElements = mutableStateMapOf<Any, MovableSharedElementData<*>>()

    @Composable
    override fun RouteIn(container: Adaptive.Container?) {
        val slot = container?.let(navigationState::slotFor)
        slotsToRoutes.getValue(slot).invoke()
    }

    fun isCurrentlyShared(key: Any): Boolean =
        keysToSharedElements.contains(key)

    fun <T> createOrUpdateSharedElement(
        key: Any,
        sharedElement: @Composable (T, Modifier) -> Unit,
    ): @Composable (T, Modifier) -> Unit {
        val movableSharedElementData = keysToSharedElements.getOrPut(key) {
            MovableSharedElementData(
                sharedElement = sharedElement,
                onRemoved = { keysToSharedElements.remove(key) }
            )
        }
        // Can't really guarantee that the caller will use the same key for the right type
        return movableSharedElementData.moveableSharedElement
    }
}

/**
 * Renders [slot] into is [Adaptive.Container] with scopes that allow for animations
 * and shared elements.
 */
@Composable
private fun SavedStateAdaptiveContentState.Render(
    slot: Adaptive.Slot,
) {
    val containerTransition = updateTransition(
        targetState = navigationState.containerStateFor(slot),
        label = "$slot-ContainerTransition",
    )
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

        when (val route = targetContainerState.currentRoute) {
            null -> Unit
            else -> Box(
                modifier = modifierFor(
                    adaptiveRouter = adaptiveRouter,
                    containerState = targetContainerState,
                    windowSizeClass = navigationState.windowSizeClass
                )
            ) {
                CompositionLocalProvider(
                    LocalAdaptiveContentScope provides scope
                ) {
                    SaveableStateProvider(route.id) {
                        adaptiveRouter.destination(route).invoke()
                        DisposableEffect(Unit) {
                            onDispose {
                                val routeIds = navigationState.routeIds
                                if (!routeIds.contains(route.id)) removeState(route.id)
                            }
                        }
                    }
                }
            }
        }

        // Add routes ids that are animating out
        LaunchedEffect(transition.isRunning) {
            if (transition.targetState == EnterExitState.PostExit) {
                val routeId = targetContainerState.currentRoute?.id ?: return@LaunchedEffect
                onAction(Action.RouteExitStart(routeId))
            }
        }
        // Remove route ids that have animated out
        DisposableEffect(Unit) {
            onDispose {
                val routeId = targetContainerState.currentRoute?.id ?: return@onDispose
                onAction(Action.RouteExitEnd(routeId))
            }
        }
    }
}

@Composable
private fun AnimatedVisibilityScope.modifierFor(
    adaptiveRouter: AdaptiveRouter,
    containerState: Adaptive.ContainerState,
    windowSizeClass: WindowSizeClass,
) = when (containerState.container) {
    Adaptive.Container.Primary, Adaptive.Container.Secondary -> FillSizeModifier
        .background(color = MaterialTheme.colorScheme.surface)
        .then(
            when {
                windowSizeClass > WindowSizeClass.COMPACT -> Modifier.clip(
                    RoundedCornerShape(16.dp)
                )

                else -> Modifier
            }
        )
        .then(
            when (val enterAndExit = adaptiveRouter.transitionsFor(containerState)) {
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