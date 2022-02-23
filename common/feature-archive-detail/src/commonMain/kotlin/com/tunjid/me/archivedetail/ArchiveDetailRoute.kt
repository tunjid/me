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

package com.tunjid.me.archivedetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.material.MaterialRichText
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.data.di.DataComponent
import com.tunjid.me.feature.Feature
import com.tunjid.me.feature.LocalRouteServiceLocator
import com.tunjid.me.scaffold.di.ScaffoldComponent
import com.tunjid.me.scaffold.di.restoredState
import com.tunjid.me.scaffold.globalui.*
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.nav.LocalNavigator
import com.tunjid.me.scaffold.nav.RouteParser
import com.tunjid.me.scaffold.nav.routeParser
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.push
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

object ArchiveDetailFeature : Feature<ArchiveDetailRoute, ArchiveDetailMutator> {

    override val routeType: KClass<ArchiveDetailRoute>
        get() = ArchiveDetailRoute::class

    override val routeParsers: List<RouteParser<ArchiveDetailRoute>> = listOf(
        routeParser(
            pattern = "archives/(.*?)/(.*?)",
            routeMapper = { result ->
                val kindString = result.groupValues.getOrNull(1)
                val archiveId = ArchiveId(result.groupValues.getOrNull(2) ?: "")
                val kind = ArchiveKind.values().firstOrNull { it.type == kindString } ?: ArchiveKind.Articles
                ArchiveDetailRoute(
                    id = result.groupValues[0],
                    kind = kind,
                    archiveId = archiveId
                )
            }
        )
    )

    override fun mutator(
        scope: CoroutineScope,
        route: ArchiveDetailRoute,
        scaffoldComponent: ScaffoldComponent,
        dataComponent: DataComponent
    ): ArchiveDetailMutator = archiveDetailMutator(
        scope = scope,
        route = route,
        initialState = scaffoldComponent.restoredState(route),
        archiveRepository = dataComponent.archiveRepository,
        authRepository = dataComponent.authRepository,
        uiStateFlow = scaffoldComponent.globalUiStateStream,
        lifecycleStateFlow = scaffoldComponent.lifecycleStateStream,
    )
}

@Serializable
data class ArchiveDetailRoute(
    override val id: String,
    val kind: ArchiveKind,
    val archiveId: ArchiveId
) : AppRoute {
    @Composable
    override fun Render() {
        ArchiveDetailScreen(
            mutator = LocalRouteServiceLocator.current.locate(this),
        )
    }

    override fun navRailRoute(nav: MultiStackNav): AppRoute? {
        val activeStack = nav.stacks.getOrNull(nav.currentIndex) ?: return null
        val previous = activeStack.routes
            .getOrNull(activeStack.routes.lastIndex - 1) as? AppRoute
            ?: return null
        return if (previous.id == "archives/${kind.type}") previous else null
    }
}

@Composable
private fun ArchiveDetailScreen(mutator: ArchiveDetailMutator) {
    val state by mutator.state.collectAsState()
    val scrollState = rememberScrollState()
    val navBarSizeDp = with(LocalDensity.current) { state.navBarSize.toDp() }
    val canEdit = state.canEdit
    val navigator = LocalNavigator.current
    ScreenUiState(
        UiState(
            toolbarShows = true,
            toolbarTitle = state.archive?.title ?: "Detail",
            navVisibility = NavVisibility.GoneIfBottomNav,
            // Prevents UI from jittering as load starts
            fabShows = if (state.hasFetchedAuthStatus) canEdit else currentUiState.fabShows,
            fabExtended = true,
            fabText = "Edit",
            fabIcon = Icons.Default.Edit,
            fabClickListener = {
                val archiveId = state.archive?.id
                if (archiveId != null) navigator.navigate {
                    currentNav.push("archives/${state.kind.type}/${archiveId.value}/edit".toRoute)
                }
            },
            insetFlags = InsetFlags.NO_BOTTOM,
            statusBarColor = MaterialTheme.colors.primary.toArgb(),
        )
    )

    val archive = state.archive

    Column(
        modifier = Modifier
            .verticalScroll(state = scrollState),
    ) {
        Spacer(modifier = Modifier.padding(8.dp))
        MaterialRichText(
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            if (archive != null) Markdown(
                content = archive.body
            )
        }
        Spacer(modifier = Modifier.padding(8.dp + navBarSizeDp))
    }
}
