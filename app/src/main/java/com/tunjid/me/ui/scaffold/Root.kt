package com.tunjid.me.ui.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.tunjid.me.LocalAppDependencies
import com.tunjid.me.AppDeps
import com.tunjid.me.globalui.UiState
import com.tunjid.me.globalui.bottomNavPositionalState
import com.tunjid.me.globalui.fragmentContainerState
import com.tunjid.me.globalui.toolbarState
import com.tunjid.me.ui.mapState

@Composable
fun Root(appDeps: AppDeps) {
    CompositionLocalProvider(LocalAppDependencies provides appDeps) {
        val rootScope = rememberCoroutineScope()
        val uiStateFlow = LocalAppDependencies.current.globalUiMutator.state
        val navStateFlow = LocalAppDependencies.current.navMutator.state

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