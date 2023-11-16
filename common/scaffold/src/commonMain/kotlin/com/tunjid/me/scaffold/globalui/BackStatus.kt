package com.tunjid.me.scaffold.globalui

import androidx.compose.runtime.Composable

interface BackStatus {
    val previewState: PreviewState
    enum class PreviewState {
        NoPreview, Previewing, CancelledAfterPreview, CommittedAfterPreview,
    }

    data class None(
        override val previewState: PreviewState = PreviewState.NoPreview
    ) : BackStatus
}

internal expect class PreviewBackStatus : BackStatus

expect val BackStatus.touchX: Float
expect val BackStatus.touchY: Float
expect val BackStatus.progress: Float
expect val BackStatus.isFromLeft: Boolean


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
