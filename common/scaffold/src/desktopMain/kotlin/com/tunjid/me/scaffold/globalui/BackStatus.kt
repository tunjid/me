package com.tunjid.me.scaffold.globalui

import androidx.compose.runtime.Composable

internal actual class PreviewBackStatus : BackStatus {
    override val previewState: BackStatus.PreviewState
        get() = BackStatus.PreviewState.NoPreview
}

actual val BackStatus.touchX: Float get() = 0F
actual val BackStatus.touchY: Float get() = 0F
actual val BackStatus.progress: Float get() = 0F
actual val BackStatus.isFromLeft: Boolean get() = false

@Composable
actual fun BackHandler(
    enabled: Boolean,
    onStarted: () -> Unit,
    onProgressed: (BackStatus) -> Unit,
    onCancelled: () -> Unit,
    onBack: () -> Unit
) = Unit