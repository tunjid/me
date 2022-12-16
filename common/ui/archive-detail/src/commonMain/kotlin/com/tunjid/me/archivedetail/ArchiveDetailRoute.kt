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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.material.MaterialRichText
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.ui.Chips
import com.tunjid.me.core.ui.Thumbnail
import com.tunjid.me.feature.LocalScreenStateHolderCache
import com.tunjid.me.scaffold.globalui.*
import com.tunjid.me.scaffold.lifecycle.collectAsStateWithLifecycle
import com.tunjid.me.scaffold.lifecycle.uiState
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.treenav.pop
import com.tunjid.treenav.push
import kotlinx.serialization.Serializable

@Serializable
data class ArchiveDetailRoute(
    override val id: String,
    val kind: ArchiveKind,
    val archiveId: ArchiveId
) : AppRoute {
    @Composable
    override fun Render() {
        ArchiveDetailScreen(
            mutator = LocalScreenStateHolderCache.current.screenStateHolderFor(this),
        )
    }

    override val navRailRoute get() = "archives/${kind.type}"
}

@Composable
private fun ArchiveDetailScreen(mutator: ArchiveDetailStateHolder) {
    val state by mutator.uiState()
    val scrollState = rememberScrollState()
    val navBarSizeDp = with(LocalDensity.current) { state.navBarSize.toDp() }

    val canEdit = state.canEdit

//    val navigator = LocalNavigator.current
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
            fabClickListener = rememberFunction(state.archive?.id) {
                val archiveId = state.archive?.id
                if (archiveId != null) mutator.accept(Action.Navigate {
                    mainNav.push("archives/${state.kind.type}/${archiveId.value}/edit".toRoute)
                })
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
        Spacer(modifier = Modifier.padding(16.dp))
        Thumbnail(
            imageUrl = state.archive?.thumbnail,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp)
        )
        Chips(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            name = "Categories:",
            chips = state.archive?.categories?.map(Descriptor.Category::value) ?: listOf(),
            color = MaterialTheme.colors.primaryVariant,
        )

        Spacer(modifier = Modifier.padding(16.dp))

        MaterialRichText(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (archive != null) Markdown(
                content = archive.body
            )
        }

        Spacer(modifier = Modifier.padding(16.dp))

        Chips(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            name = "Tags:",
            chips = state.archive?.tags?.map(Descriptor.Tag::value) ?: listOf(),
            color = MaterialTheme.colors.secondary,
        )

        Spacer(modifier = Modifier.padding(64.dp + navBarSizeDp))
    }

    // Pop nav if this archive does not exist anymore
    val wasDeleted = state.wasDeleted
    LaunchedEffect(wasDeleted) {
        if (wasDeleted) mutator.accept(Action.Navigate { mainNav.pop() })
    }
}
