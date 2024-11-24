package com.tunjid.me.scaffold.scaffold

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.tunjid.composables.backpreview.BackPreviewState
import com.tunjid.composables.splitlayout.SplitLayoutState
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.navigation.NavItem
import com.tunjid.me.scaffold.navigation.NavigationStateHolder
import com.tunjid.me.scaffold.navigation.navItemSelected
import com.tunjid.me.scaffold.navigation.navItems
import com.tunjid.me.scaffold.navigation.unknownRoute
import com.tunjid.me.scaffold.savedstate.SavedState
import com.tunjid.me.scaffold.savedstate.SavedStateRepository
import com.tunjid.me.scaffold.scaffold.PaneAnchorState.Companion.MinPaneWidth
import com.tunjid.me.sync.di.Sync
import com.tunjid.me.sync.di.keepUpToDate
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.compose.PaneStrategy
import com.tunjid.treenav.compose.PanedNavHostConfiguration
import com.tunjid.treenav.compose.PanedNavHostScope
import com.tunjid.treenav.compose.SavedStatePanedNavHostState
import com.tunjid.treenav.compose.panedNavHostConfiguration
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
import com.tunjid.treenav.current
import com.tunjid.treenav.pop
import com.tunjid.treenav.strings.PathPattern
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteTrie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Stable
class AppState @Inject constructor(
    private val routeConfigurationMap: Map<String, @JvmSuppressWildcards PaneStrategy<ThreePane, Route>>,
    private val savedStateRepository: SavedStateRepository,
    private val navigationStateHolder: NavigationStateHolder,
    private val globalUiStateHolder: GlobalUiStateHolder,
    private val sync: Sync,
) {

    private var density = Density(1f)
    private val multiStackNavState = mutableStateOf(navigationStateHolder.state.value)
    private val uiState = mutableStateOf(globalUiStateHolder.state.value)
    private val paneRenderOrder = listOf(
        ThreePane.Secondary,
        ThreePane.Primary,
    )

    val navItems by derivedStateOf { multiStackNavState.value.navItems }
    val globalUi by uiState
    val navigation by multiStackNavState
    val backPreviewState = BackPreviewState()
    val splitLayoutState = SplitLayoutState(
        orientation = Orientation.Horizontal,
        maxCount = paneRenderOrder.size,
        minSize = MinPaneWidth,
        keyAtIndex = { index ->
            val indexDiff = paneRenderOrder.size - visibleCount
            paneRenderOrder[index + indexDiff]
        }
    )

    internal val paneAnchorState by lazy { PaneAnchorState(density) }
    internal val dragToPopState = DragToPopState()

    internal val isPreviewingBack get() = !backPreviewState.progress.isNaN()
            || dragToPopState.isDraggingToPop

    fun filteredPaneOrder(
        panedNavHostScope: PanedNavHostScope<ThreePane, Route>
    ): List<ThreePane> {
        val order = paneRenderOrder.filter { panedNavHostScope.nodeFor(it) != null }
        return order
    }

    private val configurationTrie = RouteTrie<PaneStrategy<ThreePane, Route>>().apply {
        routeConfigurationMap
            .mapKeys { (template) -> PathPattern(template) }
            .forEach(::set)
    }

    private val navHostConfiguration = panedNavHostConfiguration(
        navigationState = multiStackNavState,
        destinationTransform = { multiStackNav ->
            multiStackNav.current as? Route ?: unknownRoute("")
        },
        strategyTransform = { node ->
            configurationTrie[node] ?: threePaneListDetailStrategy(
                paneMapping = { emptyMap() },
                render = {},
            )
        }
    )

    @Composable
    fun rememberPanedNavHostState(
        configurationBlock: PanedNavHostConfiguration<
                ThreePane,
                MultiStackNav,
                Route
                >.() -> PanedNavHostConfiguration<ThreePane, MultiStackNav, Route>
    ): SavedStatePanedNavHostState<ThreePane, Route> {
        LocalDensity.current.also { density = it }
        val adaptiveNavHostState = remember {
            SavedStatePanedNavHostState(
                panes = ThreePane.entries.toList(),
                configuration = navHostConfiguration.configurationBlock(),
            )
        }
        DisposableEffect(Unit) {
            val job = CoroutineScope(Dispatchers.Main.immediate).launch {
                combine(
                    navigationStateHolder.state,
                    globalUiStateHolder.state,
                    ::Pair,
                ).collect { (multiStackNav, ui) ->
                    uiState.value = ui
                    multiStackNavState.value = multiStackNav
                }
            }
            onDispose { job.cancel() }
        }
        LaunchedEffect(multiStackNavState.value) {
            savedStateRepository.saveState(multiStackNavState.value.toSavedState())
        }
        LaunchedEffect(Unit) {
            sync.keepUpToDate()
        }
        return adaptiveNavHostState
    }

    fun updateGlobalUi(
        block: UiState.() -> UiState
    ) {
        globalUiStateHolder.accept(block)
    }

    fun onNavItemSelected(navItem: NavItem) {
        navigationStateHolder.accept { navState.navItemSelected(item = navItem) }
    }

    fun pop() =
        navigationStateHolder.accept {
            navState.pop()
        }
}

internal val LocalAppState = staticCompositionLocalOf<AppState> {
    TODO()
}

private fun MultiStackNav.toSavedState() = SavedState(
    isEmpty = false,
    activeNav = currentIndex,
    navigation = stacks.fold(listOf()) { listOfLists, stackNav ->
        listOfLists.plus(
            element = stackNav.children
                .filterIsInstance<Route>()
                .fold(listOf()) { stackList, route ->
                    stackList + route.routeParams.pathAndQueries
                }
        )
    },
    routeStates = emptyMap()
)