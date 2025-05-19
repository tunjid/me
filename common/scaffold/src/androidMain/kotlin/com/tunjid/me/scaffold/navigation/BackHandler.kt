/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.me.scaffold.navigation

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import com.tunjid.me.scaffold.scaffold.AppState
import com.tunjid.treenav.pop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed


@Composable
actual fun BackHandler(
    enabled: Boolean,
    onStarted: () -> Unit,
    onProgressed: (progress: Float) -> Unit,
    onCancelled: () -> Unit,
    onBack: () -> Unit,
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
