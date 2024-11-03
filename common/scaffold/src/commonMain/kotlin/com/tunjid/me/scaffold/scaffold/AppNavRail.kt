package com.tunjid.scaffold.scaffold

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.me.scaffold.countIf
import com.tunjid.me.scaffold.globalui.navRailWidth
import com.tunjid.me.scaffold.globalui.slices.UiChromeState
import com.tunjid.me.scaffold.navigation.NavItem

/**
 * Motionally intelligent nav rail shared amongst nav routes in the app
 */
@Composable
internal fun AppNavRail(
    navItems: List<NavItem>,
    uiChromeState: UiChromeState,
    onNavItemSelected: (NavItem) -> Unit,
) {
    val statusBarSize = with(LocalDensity.current) {
        uiChromeState.statusBarSize.toDp()
    } countIf uiChromeState.insetDescriptor.hasTopInset

    val topClearance by animateDpAsState(targetValue = statusBarSize)
    val navRailWidth by animateDpAsState(
        targetValue = uiChromeState.windowSizeClass.navRailWidth() countIf uiChromeState.navRailVisible
    )

    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .width(navRailWidth),
    ) {
        Spacer(
            modifier = Modifier
                .padding(top = topClearance)
                .height(24.dp)
        )
        navItems.forEach { navItem ->
            NavRailItem(
                item = navItem,
                onNavItemSelected = onNavItemSelected
            )
        }
    }
}

@Composable
private fun NavRailItem(
    item: NavItem,
    onNavItemSelected: (NavItem) -> Unit,
) {
    val alpha = if (item.selected) 1f else 0.6f
    NavigationRailItem(
        selected = item.selected,
        icon = {
            Icon(
                imageVector = item.icon,
                contentDescription = item.name,
            )
        },
        label = {
            Text(
                modifier = Modifier.alpha(alpha),
                text = item.name,
                fontSize = 12.sp
            )
        },
        onClick = {
            onNavItemSelected(item)
        }
    )
}