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

package com.tunjid.me.archiveedit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.data.di.DataComponent
import com.tunjid.me.feature.Feature
import com.tunjid.me.feature.LocalRouteServiceLocator
import com.tunjid.me.scaffold.di.ScaffoldComponent
import com.tunjid.me.scaffold.globalui.InsetFlags
import com.tunjid.me.scaffold.globalui.NavVisibility
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.globalui.ScreenUiState
import com.tunjid.me.scaffold.nav.RouteParser
import com.tunjid.me.scaffold.nav.routeParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

object ArchiveEditFeature : Feature<ArchiveEditRoute, ArchiveEditMutator> {

    override val routeType: KClass<ArchiveEditRoute>
        get() = ArchiveEditRoute::class

    override val routeParsers: List<RouteParser<ArchiveEditRoute>> = listOf(
        routeParser(
            pattern = "archives/(.*?)/(.*?)/edit",
            routeMapper = { result ->
                val kindString = result.groupValues.getOrNull(1)
                val kind = ArchiveKind.values().firstOrNull { it.type == kindString } ?: ArchiveKind.Articles
                ArchiveEditRoute(
                    kind = kind,
                    archiveId = ArchiveId(result.groupValues.getOrNull(2) ?: "")
                )
            }
        ),
        routeParser(
            pattern = "archives/(.*?)/create",
            routeMapper = { result ->
                val kindString = result.groupValues.getOrNull(1)
                val kind = ArchiveKind.values().firstOrNull { it.type == kindString } ?: ArchiveKind.Articles
                ArchiveEditRoute(
                    kind = kind,
                    archiveId = null
                )
            }
        )
    )

    override fun mutator(
        scope: CoroutineScope,
        route: ArchiveEditRoute,
        scaffoldComponent: ScaffoldComponent,
        dataComponent: DataComponent
    ): ArchiveEditMutator = archiveEditMutator(
        scope = scope,
        initialState = null,
        route = route,
        archiveRepository = dataComponent.archiveRepository,
        authRepository = dataComponent.authRepository,
        uiStateFlow = scaffoldComponent.globalUiStateStream,
        lifecycleStateFlow = scaffoldComponent.lifecycleStateStream,
    )
}

@Serializable
data class ArchiveEditRoute(
    val kind: ArchiveKind,
    val archiveId: ArchiveId?
) : AppRoute {
    override val id: String
        get() = "archive-edit-$kind-$archiveId"

    @Composable
    override fun Render() {
        ArchiveEditScreen(
            mutator = LocalRouteServiceLocator.current.locate(this),
        )
    }
}

@Composable
private fun ArchiveEditScreen(mutator: ArchiveEditMutator) {
    val state by mutator.state.collectAsState()
    val upsert = state.upsert
    val scrollState = rememberScrollState()
    val navBarSizeDp = with(LocalDensity.current) { state.navBarSize.toDp() }

    ScreenUiState(
        UiState(
            toolbarShows = true,
            toolbarTitle = "${if (state.upsert.id == null) "Create" else "Edit"} ${state.kind.name}",
            fabShows = true,
            fabText = if (state.upsert.id == null) "Create" else "Save",
            fabIcon = Icons.Default.Done,
            fabExtended = true,
            fabEnabled = !state.isSubmitting,
            fabClickListener = {
                mutator.accept(
                    Action.Load.Submit(
                        kind = state.kind,
                        upsert = state.upsert
                    )
                )
            },
            snackbarMessages = state.messages,
            snackbarMessageConsumer = {
                mutator.accept(Action.MessageConsumed(it))
            },
            navVisibility = NavVisibility.GoneIfBottomNav,
            insetFlags = InsetFlags.NO_BOTTOM,
            statusBarColor = MaterialTheme.colors.primary.toArgb(),
        )
    )

    Column(
        modifier = Modifier
            .verticalScroll(state = scrollState),
    ) {
        Spacer(modifier = Modifier.padding(8.dp))
        TextField(
            value = upsert.title,
            maxLines = 2,
            colors = Unstyled(),
            textStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colors.onSurface,
                fontSize = 24.sp
            ),
            label = { Text(text = "Title", fontSize = 24.sp) },
            onValueChange = { mutator.accept(Action.TextEdit.Title(it)) }
        )
        Spacer(modifier = Modifier.padding(8.dp))

        TextField(
            value = upsert.description,
            maxLines = 2,
            colors = Unstyled(),
            textStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colors.onSurface,
                fontSize = 18.sp
            ),
            label = { Text(text = "Description", fontSize = 18.sp) },
            onValueChange = { mutator.accept(Action.TextEdit.Description(it)) }
        )
        Spacer(modifier = Modifier.padding(8.dp))

        EditChips(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp),
            upsert = state.upsert,
            state = state.chipsState,
            onChanged = mutator.accept
        )
        Spacer(modifier = Modifier.padding(16.dp))

        TextField(
            value = upsert.body,
            colors = Unstyled(),
            textStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colors.onSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                lineHeight = 24.sp
            ),
            label = { Text(text = "Body") },
            onValueChange = { mutator.accept(Action.TextEdit.Body(it)) }
        )
        Spacer(modifier = Modifier.padding(8.dp + navBarSizeDp))
    }
}

@Composable
private fun Unstyled() = TextFieldDefaults.textFieldColors(
    backgroundColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = MaterialTheme.colors.onSurface,
)