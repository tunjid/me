package com.tunjid.me.scaffold.globalui

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tunjid.me.scaffold.nav.NavStateHolder
import com.tunjid.treenav.pop
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal actual data class PreviewBackStatus(
    val touchX: Float,
    val touchY: Float,
    val progress: Float,
    val isFromLeft: Boolean,
    val isPreviewing: Boolean,
) : BackStatus

fun ComponentActivity.integrateBackActions(
    globalUiStateHolder: GlobalUiStateHolder,
    navStateHolder: NavStateHolder,
) {
    val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackProgressed(backEvent: BackEventCompat) {
            globalUiStateHolder.accept {
                copy(
                    backStatus = PreviewBackStatus(
                        touchX = backEvent.touchX,
                        touchY = backEvent.touchY,
                        progress = backEvent.progress,
                        isPreviewing = true,
                        isFromLeft = backEvent.swipeEdge == BackEventCompat.EDGE_LEFT
                    )
                )
            }
        }

        override fun handleOnBackPressed() {
            // Dismiss back preview
            globalUiStateHolder.accept { copy(backStatus = BackStatus.None) }
            // Pop navigation
            navStateHolder.accept { navState.pop() }
        }

        override fun handleOnBackCancelled() {
            globalUiStateHolder.accept { copy(backStatus = BackStatus.None) }
        }
    }
    onBackPressedDispatcher.addCallback(backPressedCallback)

    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            navStateHolder.state.map { navState ->
                // If the nav stack can be popped, then this callback is enabled
                navState.mainNav != navState.mainNav.pop()
            }
                .distinctUntilChanged()
                .collect(backPressedCallback::isEnabled::set)
        }
    }
}

actual val BackStatus.touchX: Float
    get() =
        if (this is PreviewBackStatus) touchX else 0F
actual val BackStatus.touchY: Float
    get() =
        if (this is PreviewBackStatus) touchY else 0F
actual val BackStatus.progress: Float
    get() =
        if (this is PreviewBackStatus) progress else 0F
actual val BackStatus.isFromLeft: Boolean
    get() =
        if (this is PreviewBackStatus) isFromLeft else false

actual val BackStatus.isPreviewing: Boolean
    get() =
        if (this is PreviewBackStatus) isPreviewing else false