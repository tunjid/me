package com.tunjid.me.scaffold.globalui

import androidx.compose.runtime.Composable

internal actual class PreviewBackStatus : BackStatus

actual val BackStatus.touchX: Float get() = 0F
actual val BackStatus.touchY: Float get() = 0F
actual val BackStatus.progress: Float get() = 0F
actual val BackStatus.isFromLeft: Boolean get() = false

actual val BackStatus.isPreviewing: Boolean get() = false