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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.me.common.app.AppMutator
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.globalui.fabState
import com.tunjid.me.common.ui.utilities.UiSizes
import com.tunjid.me.common.ui.utilities.countIf
import com.tunjid.me.common.ui.utilities.mappedCollectAsState

@Composable
internal fun BoxScope.AppFab(
    appMutator: AppMutator,
) {
    val globalUiMutator = appMutator.globalUiMutator
    val state by globalUiMutator.state.mappedCollectAsState(mapper = UiState::fabState)
    val clicks by globalUiMutator.state.mappedCollectAsState(mapper = UiState::fabClickListener)

    val fabPosition by animateDpAsState(
        when {
            !state.fabVisible -> UiSizes.bottomNavSize
            else -> {
                var offset = 16.dp
                offset += state.snackbarOffset + UiSizes.bottomNavSize countIf state.bottomNavVisible
                offset += state.snackbarOffset

                -offset
            }
        }
    )

    FloatingActionButton(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(x = (-16).dp, y = fabPosition)
            .wrapContentHeight(),
        onClick = { clicks(Unit) },
        content = {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = state.icon, contentDescription = null)
                if (state.extended) Spacer(modifier = Modifier.width(16.dp))
                AnimatedVisibility(visible = state.extended) {
                    Text(text = state.text)
                }
            }
        }
    )
}
