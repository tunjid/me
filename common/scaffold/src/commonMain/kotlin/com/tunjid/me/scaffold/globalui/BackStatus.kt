package com.tunjid.me.scaffold.globalui

import androidx.compose.runtime.Composable

interface BackStatus {
    data object None: BackStatus
    data object DragDismiss : BackStatus
}

internal expect class PreviewBackStatus: BackStatus

expect val BackStatus.touchX: Float
expect val BackStatus.touchY: Float
expect val BackStatus.progress: Float
expect val BackStatus.isFromLeft: Boolean
expect val BackStatus.isPreviewing: Boolean


/**
 * Mirror of Android platform back handler API
 */
@Composable
expect fun BackHandler(
    enabled: Boolean = true,
    onStarted: () -> Unit = {},
    onProgressed: (BackStatus) -> Unit = {},
    onCancelled: () -> Unit = {},
    onBack: () -> Unit
)
