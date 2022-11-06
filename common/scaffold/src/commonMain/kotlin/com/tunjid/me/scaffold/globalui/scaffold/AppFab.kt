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

package com.tunjid.me.scaffold.globalui.scaffold

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tunjid.me.core.utilities.countIf
import com.tunjid.me.core.utilities.mappedCollectAsState
import com.tunjid.me.scaffold.globalui.GlobalUiMutator
import com.tunjid.me.scaffold.globalui.UiSizes
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.keyboardSize
import com.tunjid.me.scaffold.globalui.slices.fabState
import com.tunjid.me.scaffold.lifecycle.mappedCollectAsStateWithLifecycle

/**
 * Common motionally intelligent Floating Action button shared amongst nav routes in the app
 *
 */
@Composable
internal fun BoxScope.AppFab(
    globalUiMutator: GlobalUiMutator,
) {
    val state by globalUiMutator.state.mappedCollectAsStateWithLifecycle(mapper = UiState::fabState)
    val clicks by globalUiMutator.state.mappedCollectAsStateWithLifecycle(mapper = UiState::fabClickListener)
    val enabled = state.enabled
    val position by animateDpAsState(
        when {
            state.fabVisible -> -with(LocalDensity.current) {
                16.dp + when {
                    state.keyboardSize > 0 -> state.keyboardSize.toDp() +
                            state.snackbarOffset

                    else -> (UiSizes.bottomNavSize countIf state.bottomNavVisible) +
                            state.navBarSize.toDp() +
                            state.snackbarOffset
                }
            }

            else -> UiSizes.bottomNavSize
        }
    )

    Button(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(x = (-16).dp, y = position)
            .wrapContentHeight(),
        enabled = enabled,
        onClick = { if (enabled) clicks(Unit) },
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary
        ),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 16.dp
        ),
        shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                FabIcon(state.icon)
                if (state.extended) Spacer(modifier = Modifier.width(8.dp))
                AnimatedContent(targetState = state.text) { text ->
                    Text(text = text)
                }
            }
        }
    )
}

@Composable
private fun FabIcon(icon: ImageVector) {
    val rotationAnimatable = remember { Animatable(initialValue = 0f) }
    val animationSpec = remember {
        spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh,
            visibilityThreshold = 0.1f
        )
    }

    Icon(
        modifier = Modifier.rotate(rotationAnimatable.value),
        imageVector = icon,
        contentDescription = null
    )

    LaunchedEffect(icon) {
        rotationAnimatable.animateTo(targetValue = 30f, animationSpec = animationSpec)
        rotationAnimatable.animateTo(targetValue = 0f, animationSpec = animationSpec)
    }
}
