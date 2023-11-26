/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.me.scaffold.scaffold

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tunjid.me.core.model.peek
import com.tunjid.me.core.utilities.countIf
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.bottomNavSize
import com.tunjid.me.scaffold.globalui.keyboardSize
import com.tunjid.me.scaffold.globalui.slices.snackbarPositionalState
import com.tunjid.me.scaffold.lifecycle.mappedCollectAsStateWithLifecycle
import kotlinx.coroutines.delay

private val snackbarPeek = 56.dp

/**
 * Motionally intelligent Snackbar shared amongst nav routes in the app
 */
@Composable
internal fun BoxScope.AppSnackBar(
    globalUiStateHolder: GlobalUiStateHolder,
) {
    val queue by globalUiStateHolder.state.mappedCollectAsStateWithLifecycle(
        mapper = UiState::snackbarMessages
    )
    val state by globalUiStateHolder.state.mappedCollectAsStateWithLifecycle(
        mapper = UiState::snackbarPositionalState
    )
    val messageConsumer by globalUiStateHolder.state.mappedCollectAsStateWithLifecycle(
        mapper = UiState::snackbarMessageConsumer
    )

    var canShow by remember { mutableStateOf(true) }
    var snackbarHeight by remember { mutableStateOf(0) }
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
            messageConsumer(message)
        }
    }

    LaunchedEffect(position) {
        if (position == snackbarPeek && !canShow) canShow = true
    }

    LaunchedEffect(snackbarOffset) {
        globalUiStateHolder.accept { copy(snackbarOffset = snackbarOffset) }
    }
}