package com.tunjid.me.scaffold.scaffold

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.tunjid.me.scaffold.countIf
import com.tunjid.me.scaffold.globalui.bottomNavSize
import com.tunjid.me.scaffold.globalui.keyboardSize
import com.tunjid.me.scaffold.globalui.navRailWidth
import com.tunjid.me.scaffold.globalui.slices.UiChromeState


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