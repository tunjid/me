package com.tunjid.me.ui.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.tunjid.me.nav.MultiStackNav
import com.tunjid.me.nav.Route
import com.tunjid.me.nav.current
import com.tunjid.me.ui.mapState
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun AppNavRouter(
    navStateFlow: StateFlow<MultiStackNav>
) {
    val scope = rememberCoroutineScope()
    val routeState = navStateFlow
        .mapState(scope, MultiStackNav::current)
        .collectAsState()

    when (val route = routeState.value) {
        is Route -> route.Render()
        else -> Box {
            Text(
                modifier = Modifier
                    .padding(),
                text = "404"
            )
        }
    }
}