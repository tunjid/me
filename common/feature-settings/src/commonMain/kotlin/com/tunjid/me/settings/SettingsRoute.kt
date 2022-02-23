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

package com.tunjid.me.settings

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
import com.tunjid.me.data.di.DataComponent
import com.tunjid.me.feature.Feature
import com.tunjid.me.feature.LocalRouteServiceLocator
import com.tunjid.me.scaffold.di.ScaffoldComponent
import com.tunjid.me.scaffold.globalui.InsetFlags
import com.tunjid.me.scaffold.globalui.NavVisibility
import com.tunjid.me.scaffold.globalui.ScreenUiState
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.nav.LocalNavigator
import com.tunjid.me.scaffold.nav.RouteParser
import com.tunjid.me.scaffold.nav.routeParser
import com.tunjid.treenav.push
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

object SettingsFeature : Feature<SettingsRoute, SettingsMutator> {

    override val routeType: KClass<SettingsRoute>
        get() = SettingsRoute::class

    override val routeParsers: List<RouteParser<SettingsRoute>> = listOf(
        routeParser(
            pattern = "settings",
            routeMapper = { result ->
                SettingsRoute(
                    id = result.groupValues[0],
                )
            }
        )
    )

    override fun mutator(
        scope: CoroutineScope,
        route: SettingsRoute,
        scaffoldComponent: ScaffoldComponent,
        dataComponent: DataComponent
    ): SettingsMutator = settingsMutator(
        scope = scope,
        initialState = null,
        route = route,
        authRepository = dataComponent.authRepository,
        lifecycleStateFlow = scaffoldComponent.lifecycleStateStream,
    )
}

@Serializable
data class SettingsRoute(
    override val id: String,
) : AppRoute {
    @Composable
    override fun Render() {
        SettingsScreen(
            mutator = LocalRouteServiceLocator.current.locate(this),
        )
    }
}

@Composable
private fun SettingsScreen(mutator: SettingsMutator) {
    val state by mutator.state.collectAsState()
    val scrollState = rememberScrollState()
    val navigator = LocalNavigator.current

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
        state.routes.forEach { path ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navigator.navigate { currentNav.push(path.toRoute) }
                    },
                content = {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = path,
                    )
                }
            )
        }
    }
}

