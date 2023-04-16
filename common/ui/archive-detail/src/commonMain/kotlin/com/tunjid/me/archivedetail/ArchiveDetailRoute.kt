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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.material3.Material3RichText
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.ui.AsyncRasterImage
import com.tunjid.me.core.ui.Chips
import com.tunjid.me.feature.LocalScreenStateHolderCache
import com.tunjid.me.scaffold.lifecycle.component1
import com.tunjid.me.scaffold.lifecycle.component2
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.treenav.pop
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
            stateHolder = LocalScreenStateHolderCache.current.screenStateHolderFor(this),
        )
    }

    override val supportingRoute get() = "archives/${kind.type}"
}

@Composable
private fun ArchiveDetailScreen(stateHolder: ArchiveDetailStateHolder) {
    val (state, actions) = stateHolder
    val scrollState = rememberScrollState()
    val navBarSizeDp = with(LocalDensity.current) { state.navBarSize.toDp() }

    val canEdit = state.canEdit

//    val navigator = LocalNavigator.current
    if (state.isInMainNav) GlobalUi(
        state = state,
        actions = actions
    )

    val archive = state.archive

    Column(
        modifier = Modifier
            .verticalScroll(state = scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.padding(16.dp))
        AsyncRasterImage(
            imageUrl = state.archive?.thumbnail,
            modifier = Modifier
                .heightIn(max = 300.dp)
                .aspectRatio(ratio = 16f / 9f)
                .padding(horizontal = 16.dp)
                .clip(MaterialTheme.shapes.medium)
        )
        Chips(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            name = "Categories:",
            chipInfoList = state.descriptorChips<Descriptor.Category>(),
        )

        Spacer(modifier = Modifier.padding(16.dp))

        Material3RichText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
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
            chipInfoList = state.descriptorChips<Descriptor.Tag>(),
        )

        Spacer(modifier = Modifier.padding(64.dp + navBarSizeDp))
    }

    // Pop nav if this archive does not exist anymore
    val wasDeleted = state.wasDeleted
    LaunchedEffect(wasDeleted) {
        if (wasDeleted) actions(Action.Navigate { mainNav.pop() })
    }
}
