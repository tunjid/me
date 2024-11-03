package com.tunjid.scaffold.scaffold

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.tunjid.scaffold.countIf
import com.tunjid.scaffold.globalui.bottomNavSize
import com.tunjid.scaffold.globalui.keyboardSize
import com.tunjid.scaffold.globalui.navRailWidth
import com.tunjid.scaffold.globalui.slices.UiChromeState


@Composable
internal fun Modifier.routePanePadding(
    state: State<UiChromeState>,
): Modifier {
    val paddingValues = remember {
        mutableStateListOf(0.dp, 0.dp, 0.dp, 0.dp)
    }
    val uiChromeState by state

    val bottomNavHeight = uiChromeState.windowSizeClass
        .bottomNavSize() countIf uiChromeState.bottomNavVisible

    val insetClearance = max(
        a = bottomNavHeight,
        b = with(LocalDensity.current) { uiChromeState.keyboardSize.toDp() }
    )
    val navBarClearance = with(LocalDensity.current) {
        uiChromeState.navBarSize.toDp()
    } countIf uiChromeState.insetDescriptor.hasBottomInset

    val bottomClearance by animateDpAsState(
        label = "Bottom clearance animation",
        targetValue = insetClearance + navBarClearance,
        animationSpec = PaneSizeSpring
    )

    val navRailSize =
        uiChromeState.windowSizeClass.navRailWidth() countIf uiChromeState.navRailVisible

    val startClearance by animateDpAsState(
        label = "Start clearance animation",
        targetValue = navRailSize,
        animationSpec = PaneSizeSpring
    )

    paddingValues[0] = startClearance
    paddingValues[3] = bottomClearance

    return padding(
        start = paddingValues[0],
        top = paddingValues[1],
        end = paddingValues[2],
        bottom = paddingValues[3]
    )
}

/**
 * Shifts layouts out of view when the content area is too small instead of resizing them
 */
internal fun Modifier.restrictedSizePlacement(
    atStart: Boolean
) = layout { measurable, constraints ->
    val minPanWidth = MinPaneLayoutWidth.roundToPx()
    val actualConstraints = when {
        constraints.maxWidth < minPanWidth -> constraints.copy(maxWidth = minPanWidth)
        else -> constraints
    }
    val placeable = measurable.measure(actualConstraints)
    layout(width = placeable.width, height = placeable.height) {
        placeable.placeRelative(
            x = if (constraints.maxWidth < minPanWidth) when {
                atStart -> constraints.maxWidth - minPanWidth
                else -> minPanWidth - constraints.maxWidth
            } else 0,
            y = 0,
        )
    }
}

private val MinPaneLayoutWidth = 120.dp

private val PaneSizeSpring = spring(
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = Dp.VisibilityThreshold
)

@Composable
internal inline fun <T> rememberUpdatedStateIf(
    value: T,
    predicate: (T) -> Boolean
): State<T> = remember {
    mutableStateOf(value)
}.also { if (predicate(value)) it.value = value }