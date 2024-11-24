package com.tunjid.me.scaffold.globalui

import androidx.compose.runtime.Composable

/**
 * Mirror of Android platform back handler API
 */
@Composable
expect fun BackHandler(
    enabled: Boolean = true,
    onStarted: () -> Unit = {},
    onProgressed: (Float) -> Unit = {},
    onCancelled: () -> Unit = {},
    onBack: () -> Unit
)
