package com.tunjid.me.ui.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.tunjid.me.AppDependencies
import com.tunjid.me.AppDeps
import com.tunjid.me.globalui.UiState
import com.tunjid.me.globalui.bottomNavPositionalState
import com.tunjid.me.globalui.fragmentContainerState
import com.tunjid.me.globalui.toolbarState
import com.tunjid.me.ui.mapState
import com.tunjid.me.ui.scaffold.AppNavRouter

@Composable
fun Root(appDeps: AppDeps) {
    CompositionLocalProvider(AppDependencies provides appDeps) {
        val rootScope = rememberCoroutineScope()
        val uiStateFlow = AppDependencies.current.globalUiMutator.state
        val navStateFlow = AppDependencies.current.navMutator.state

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AppToolbar(
                stateFlow = uiStateFlow.mapState(
                    scope = rootScope,
                    mapper = UiState::toolbarState
                )
            )

            AppRouteContainer(
                stateFlow = uiStateFlow.mapState(
                    scope = rootScope,
                    mapper = UiState::fragmentContainerState
                ),
                content = {
                    AppNavRouter(
                        navStateFlow = navStateFlow
                    )
                }
            )
            AppBottomNav(
                stateFlow = uiStateFlow.mapState(
                    scope = rootScope,
                    mapper = UiState::bottomNavPositionalState
                )
            )
        }
    }
}