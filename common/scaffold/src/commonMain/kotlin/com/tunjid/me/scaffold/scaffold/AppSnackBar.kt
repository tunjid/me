package com.tunjid.scaffold.scaffold

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.me.core.model.Message
import com.tunjid.me.core.model.MessageQueue
import com.tunjid.me.core.model.peek
import com.tunjid.me.scaffold.countIf
import com.tunjid.me.scaffold.globalui.bottomNavSize
import com.tunjid.me.scaffold.globalui.keyboardSize
import com.tunjid.me.scaffold.globalui.slices.SnackbarPositionalState
import kotlinx.coroutines.delay

private val snackbarPeek = 56.dp

/**
 * Motionally intelligent Snackbar shared amongst nav routes in the app
 */
@Composable
internal fun BoxScope.AppSnackBar(
    state: SnackbarPositionalState,
    queue: MessageQueue,
    onMessageClicked: (Message) -> Unit,
    onSnackbarOffsetChanged: (Dp) -> Unit,
) {
    var canShow by remember { mutableStateOf(true) }
    var snackbarHeight by remember { mutableIntStateOf(0) }
    val message = queue.peek()?.takeIf { canShow }
    val head = message?.value

    val showing = head != null
    val position by animateDpAsState(
        if (showing) -with(LocalDensity.current) {
            16.dp + when {
                state.keyboardSize > 0 -> state.keyboardSize.toDp()
                else -> state.windowSizeClass.bottomNavSize() countIf state.bottomNavVisible
            }
        }
        else snackbarPeek
    )
    val snackbarOffset by animateDpAsState(
        if (showing) with(LocalDensity.current) {
            snackbarHeight.toDp() + (16.dp countIf (state.keyboardSize > 0))
        }
        else 0.dp
    )

    Snackbar(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 16.dp)
            .widthIn(max = 400.dp)
            .onSizeChanged { snackbarHeight = it.height }
            .offset(y = position),
        content = { Text(text = head ?: "") }
    )

    LaunchedEffect(head) {
        if (head != null) {
            delay(1000)
            canShow = false
            onMessageClicked(message)
        }
    }

    LaunchedEffect(position) {
        if (position == snackbarPeek && !canShow) canShow = true
    }

    LaunchedEffect(snackbarOffset) {
        onSnackbarOffsetChanged(snackbarOffset)
    }
}