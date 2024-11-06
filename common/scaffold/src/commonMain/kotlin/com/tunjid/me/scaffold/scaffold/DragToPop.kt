package com.tunjid.scaffold.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.tunjid.composables.dragtodismiss.dragToDismiss
import com.tunjid.me.scaffold.globalui.BackStatus
import com.tunjid.me.scaffold.scaffold.rememberUpdatedStateIf
import com.tunjid.treenav.compose.PanedNavHostScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.strings.Route

@Composable
fun Modifier.dragToPop(): Modifier {
    val state = LocalAppState.current.dragToDismissState
    DisposableEffect(state) {
        state.enabled = true
        onDispose { state.enabled = false }
    }
    // TODO: This should not be necessary. Figure out why a frame renders with
    //  an offset of zero while the content in the transient primary container
    //  is still visible.
    val dragToDismissOffset by rememberUpdatedStateIf(
        value = state.offset.round(),
        predicate = {
            it != IntOffset.Zero
        }
    )
    return offset { dragToDismissOffset }
}

@Composable
internal fun PanedNavHostScope<ThreePane, Route>.DragToPopLayout(
    state: MeAppState,
    pane: ThreePane,
) {
    // Only place the DragToDismiss Modifier on the Primary pane
    if (pane == ThreePane.Primary) {
        Box(
            modifier = Modifier.dragToPopInternal(state)
        ) {
            Destination(pane)
        }
        // Place the transient primary screen above  the primary
        Destination(ThreePane.TransientPrimary)
    } else {
        Destination(pane)
    }
}

@Composable
private fun Modifier.dragToPopInternal(state: MeAppState): Modifier {
    val density = LocalDensity.current
    val dismissThreshold = remember { with(density) { 200.dp.toPx().let { it * it } } }

    return dragToDismiss(
        state = state.dragToDismissState,
        dragThresholdCheck = { offset, _ ->
            offset.getDistanceSquared() > dismissThreshold
        },
        // Enable back preview
        onStart = {
            state.updateGlobalUi {
                copy(backStatus = BackStatus.DragDismiss)
            }
        },
        onCancelled = {
            // Dismiss back preview
            state.updateGlobalUi {
                copy(backStatus = BackStatus.None)
            }
        },
        onDismissed = {
            // Dismiss back preview
            state.updateGlobalUi {
                copy(backStatus = BackStatus.None)
            }
            // Pop navigation
            state.pop()
        }
    )
}