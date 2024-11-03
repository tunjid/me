package com.tunjid.scaffold.scaffold

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tunjid.scaffold.globalui.bottomNavSize
import com.tunjid.scaffold.globalui.slices.BottomNavPositionalState
import com.tunjid.scaffold.navigation.NavItem

/**
 * Motionally intelligent bottom nav shared amongst nav routes in the app
 */
@Composable
internal fun BoxScope.AppBottomNav(
    navItems: List<NavItem>,
    positionalState: BottomNavPositionalState,
    onNavItemSelected: (NavItem) -> Unit,
) {
    val windowSizeClass = positionalState.windowSizeClass

    val bottomNavPosition by animateDpAsState(
        when {
            positionalState.bottomNavVisible -> 0.dp
            else -> windowSizeClass.bottomNavSize() + with(LocalDensity.current) { positionalState.navBarSize.toDp() }
        }
    )

    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .offset(y = bottomNavPosition)
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        BottomAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            windowInsets = WindowInsets(left = 0, top = 0, right = 0, bottom = 0)
        ) {

            navItems
                .forEach { navItem ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = navItem.icon,
                                contentDescription = navItem.name
                            )
                        },
                        label = { Text(navItem.name) },
                        selected = navItem.selected,
                        onClick = {
                            onNavItemSelected(navItem)
                        }
                    )
                }
        }
        BottomAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(LocalDensity.current) {
                    positionalState.navBarSize.toDp()
                })
        ) {}
    }
}
