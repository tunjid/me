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

package com.tunjid.me.common.ui.archivedetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.material.MaterialRichText
import com.tunjid.me.common.LocalAppDependencies
import com.tunjid.me.common.data.archive.Archive
import com.tunjid.me.common.globalui.InsetFlags
import com.tunjid.me.common.globalui.NavVisibility
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.nav.AppRoute
import com.tunjid.me.common.ui.InitialUiState

data class ArchiveDetailRoute(val archive: Archive) : AppRoute<ArchiveDetailMutator> {
    override val id: String
        get() = archive.key

    @Composable
    override fun Render() {
        ArchiveDetailScreen(
            mutator = LocalAppDependencies.current.routeDependencies(this)
        )
    }
}

@Composable
private fun ArchiveDetailScreen(mutator: ArchiveDetailMutator) {
    val state by mutator.state.collectAsState()
    val scrollState = rememberScrollState()
    val navBarSizeDp = with(LocalDensity.current) { state.navBarSize.toDp() }

    InitialUiState(
        UiState(
            toolbarShows = true,
            toolbarTitle = state.archive.title,
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
        MaterialRichText(
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Markdown(
                content = state.archive.body
            )
        }
        Spacer(modifier = Modifier.padding(8.dp + navBarSizeDp))
    }
}
