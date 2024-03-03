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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.me.core.ui.FormField
import com.tunjid.me.scaffold.navigation.SerializedRouteParams
import com.tunjid.treenav.strings.Route
import kotlinx.serialization.Serializable

@Serializable
data class SignInRoute(
    override val routeParams: SerializedRouteParams,
) : Route

@Composable
internal fun SignInScreen(
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    GlobalUi(
        state = state,
        onAction = actions
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(state = scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        state.fields.forEach { field ->
            Spacer(modifier = Modifier.height(8.dp))
            FormField(
                modifier = Modifier
                    .fillMaxWidth(0.6f),
                field = field,
                onValueChange = {
                    actions(Action.FieldChanged(field = field.copy(value = it)))
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
