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

package com.tunjid.me.common.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import com.tunjid.me.common.globalui.ScreenUiState
import com.tunjid.mutator.accept
import com.tunjid.treenav.push
import kotlinx.serialization.Serializable

@Serializable
object SettingsRoute : AppRoute<SettingsMutator> {
    override val id: String
        get() = "settings"

    @Composable
    override fun Render() {
        SettingsScreen(
            mutator = LocalAppDependencies.current.routeDependencies(this)
        )
    }
}

@Composable
private fun SettingsScreen(mutator: SettingsMutator) {
    val state by mutator.state.collectAsState()
    val scrollState = rememberScrollState()
    val navMutator = LocalAppDependencies.current.appMutator.navMutator

    ScreenUiState(
        UiState(
            toolbarShows = true,
            toolbarTitle = "Settings",
            navVisibility = NavVisibility.Visible,
            insetFlags = InsetFlags.NO_BOTTOM,
            statusBarColor = MaterialTheme.colors.primary.toArgb(),
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(state = scrollState),
        horizontalAlignment = Alignment.Start,
    ) {
        state.routes.forEach { route ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                    navMutator.accept { push(route) }
                },
                content = {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = route.id,
                    )
                }
            )
        }
    }
}

