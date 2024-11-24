package com.tunjid.me.scaffold.globalui

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import com.tunjid.me.scaffold.scaffold.AppState
import com.tunjid.treenav.pop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlin.coroutines.cancellation.CancellationException

@Composable
actual fun BackHandler(
    enabled: Boolean,
    onStarted: () -> Unit,
    onProgressed: (progress: Float) -> Unit,
    onCancelled: () -> Unit,
    onBack: () -> Unit
) {
    PredictiveBackHandler(enabled) { progress: Flow<BackEventCompat> ->
        try {
            progress.collectIndexed { index, backEvent ->
                if (index == 0) onStarted()
                onProgressed(backEvent.progress)
            }
            onBack()
        } catch (e: CancellationException) {
            onCancelled()
        }
    }
}

@Composable
fun PredictiveBackEffects(
    appState: AppState,
) {
    PredictiveBackHandler(
        enabled = appState.navigation.let { it != it.pop() },
        onBack = {
            try {
                it.collect { backEvent ->
                    appState.backPreviewState.apply {
                        atStart = backEvent.swipeEdge == BackEventCompat.EDGE_LEFT
                        progress = backEvent.progress
                        pointerOffset = Offset(
                            x = backEvent.touchX,
                            y = backEvent.touchY
                        ).round()
                    }
                }
                // Dismiss back preview
                appState.backPreviewState.apply {
                    progress = Float.NaN
                    pointerOffset = IntOffset.Zero
                }
                // Pop navigation
                appState.pop()
            } catch (e: CancellationException) {
                appState.backPreviewState.apply {
                    progress = Float.NaN
                    pointerOffset = IntOffset.Zero
                }
            }
        }
    )
}
