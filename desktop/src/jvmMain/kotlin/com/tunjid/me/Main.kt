package com.tunjid.me

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.tunjid.me.common.createAppDependencies
import com.tunjid.me.common.globalui.NavMode
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.ui.scaffold.Root
import com.tunjid.me.common.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun main() {
    application {
        val appDependencies = createAppDependencies(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
            initialUiState = UiState(navMode = NavMode.NavRail)
        )

        val windowState = rememberWindowState()
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Me as a composition"
        ) {
            AppTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Root(dependencies = appDependencies)
                }
            }
        }
    }
}


