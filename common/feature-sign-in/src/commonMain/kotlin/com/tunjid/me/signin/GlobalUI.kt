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

package com.tunjid.me.signin

import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import com.tunjid.me.core.ui.icons.Preview
import com.tunjid.me.scaffold.globalui.*
import com.tunjid.me.scaffold.globalui.slices.ToolbarItem

@Composable
internal fun GlobalUi(
    state: State,
    onAction: (Action) -> Unit
) {
    ScreenUiState(
        UiState(
            toolbarShows = true,
            toolbarTitle = "Sign In",
            fabShows = true,
            fabEnabled = state.submitButtonEnabled,
            fabText = "Submit",
            fabClickListener = rememberFunction(state.sessionRequest) {
                onAction(
                    Action.Submit(request = state.sessionRequest)
                )
            },
            snackbarMessages = state.messages,
            snackbarMessageConsumer = rememberFunction {
                onAction(Action.MessageConsumed(it))
            },
            navVisibility = NavVisibility.Gone,
            insetFlags = InsetFlags.NO_BOTTOM,
            statusBarColor = MaterialTheme.colors.primary.toArgb(),
        )

    )
}