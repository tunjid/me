package com.tunjid.me.scaffold.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.tunjid.scaffold.adaptive.Adaptive

@Stable
interface AdaptiveContentState {

    val navigationState: Adaptive.NavigationState

    @Composable
    fun RouteIn(container: Adaptive.Container?)
}
