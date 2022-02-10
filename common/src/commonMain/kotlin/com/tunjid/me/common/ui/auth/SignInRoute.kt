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

package com.tunjid.me.common.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.tunjid.me.common.app.LocalAppDependencies
import com.tunjid.me.common.globalui.InsetFlags
import com.tunjid.me.common.globalui.NavVisibility
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.nav.AppRoute
import com.tunjid.me.common.ui.utilities.InitialUiState
import kotlinx.serialization.Serializable

@Serializable
object SignInRoute : AppRoute<SignInMutator> {
    override val id: String
        get() = "sign-in"

    @Composable
    override fun Render() {
        SignInScreen(
            mutator = LocalAppDependencies.current.routeDependencies(this)
        )
    }
}

@Composable
private fun SignInScreen(mutator: SignInMutator) {
    val state by mutator.state.collectAsState()
    val scrollState = rememberScrollState()

    InitialUiState(
        UiState(
            toolbarShows = true,
            toolbarTitle = "Sign In",
            navVisibility = NavVisibility.Gone,
            insetFlags = InsetFlags.NO_BOTTOM,
            statusBarColor = MaterialTheme.colors.primary.toArgb(),
        )
    )


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(state = scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        state.fields.forEach { field ->
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                modifier = Modifier
                    .fillMaxWidth(0.6f),
                value = field.value,
                onValueChange = {
                    mutator.accept(Action.FieldChanged(field = field.copy(value = it)))
                },
                label = { Text(text = field.id) }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            enabled = state.submitButtonEnabled,
            onClick = {
                mutator.accept(
                    Action.Submit(request = state.sessionRequest)
                )
            },
            content = { Text(text = "Submit") }
        )
    }
}
