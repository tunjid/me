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

package com.tunjid.me.common.ui.scaffold

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.me.common.app.AppMutator
import com.tunjid.me.common.data.model.peek
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.ui.utilities.UiSizes
import com.tunjid.me.common.ui.utilities.mappedCollectAsState
import com.tunjid.mutator.accept
import kotlinx.coroutines.delay

/**
 * Motionally intelligent Snackbar shared amongst nav routes in the app
 */
@Composable
internal fun BoxScope.AppSnackBar(
    appMutator: AppMutator,
) {
    val globalUiMutator = appMutator.globalUiMutator

    val queue by globalUiMutator.state.mappedCollectAsState(mapper = UiState::snackbarMessages)
    val messageConsumer by globalUiMutator.state.mappedCollectAsState(mapper = UiState::snackbarMessageConsumer)

    var canShow by remember { mutableStateOf(true) }
    val message = queue.peek()?.takeIf { canShow }
    val head = message?.value

    val showing = head != null
    val position by animateDpAsState(if (showing) -(16).dp else UiSizes.snackbarPeek)

    Snackbar(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 16.dp)
            .widthIn(max = 400.dp)
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
        globalUiMutator.accept { copy(snackbarOffset = UiSizes.snackbarPeek - position) }
        if (position == UiSizes.snackbarPeek && !canShow) canShow = true
    }
}