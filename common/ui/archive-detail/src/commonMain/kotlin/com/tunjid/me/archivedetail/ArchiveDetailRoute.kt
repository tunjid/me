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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.ui.AsyncRasterImage
import com.tunjid.me.core.ui.Chips
import com.tunjid.me.core.ui.NestedScrollTextContainer
import com.tunjid.me.core.ui.isInViewport
import com.tunjid.me.feature.rememberRetainedStateHolder
import com.tunjid.me.scaffold.globalui.PaneAnchor
import com.tunjid.me.scaffold.adaptive.rememberSharedContent
import com.tunjid.me.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.me.scaffold.scaffold.backPreviewBackgroundModifier
import com.tunjid.me.scaffold.lifecycle.component1
import com.tunjid.me.scaffold.lifecycle.component2
import com.tunjid.me.scaffold.adaptive.AdaptiveRoute
import com.tunjid.me.scaffold.adaptive.ExternalRoute
import com.tunjid.me.scaffold.navigation.SerializedRouteParams
import com.tunjid.treenav.Node
import com.tunjid.treenav.pop
import kotlinx.serialization.Serializable

private const val BODY_KEY = 3

@Serializable
data class ArchiveDetailRoute(
    override val routeParams: SerializedRouteParams,
) : AdaptiveRoute {

    val archiveId: ArchiveId? = routeParams.pathArgs["id"]?.let(::ArchiveId)

    val kind = ArchiveKind.entries.firstOrNull { it.type == routeParams.pathArgs["kind"] }
        ?: ArchiveKind.Articles

    val archiveThumbnail: String? get() = routeParams.queryParams["thumbnail"]?.firstOrNull()

    @Composable
    override fun content() {
        ArchiveDetailScreen(
            modifier = Modifier.backPreviewBackgroundModifier(),
            stateHolder = rememberRetainedStateHolder(
                route = this@ArchiveDetailRoute
            ),
        )
    }

    override val children: List<Node> = listOf(
        ExternalRoute(
            path = "archives/${kind.type}"
        )
    )

    override val secondaryRoute get() = children.filterIsInstance<ExternalRoute>().first()
}

@Composable
private fun ArchiveDetailScreen(
    modifier: Modifier = Modifier,
    stateHolder: ArchiveDetailStateHolder
) {
    val (state, actions) = stateHolder
    val scrollState = rememberLazyListState()
    val navBarSizeDp = with(LocalDensity.current) { state.navBarSize.toDp() }
    val bodyInViewport = scrollState.isInViewport(BODY_KEY)

    GlobalUi(
        state = state,
        actions = actions
    )

    val archive = state.archive

    // Close the secondary pane when invoking back since it contains the list view
    SecondaryPaneCloseBackHandler(
        enabled = state.isInPrimaryNav && state.hasSecondaryPanel
    )

    val thumbnail = rememberSharedContent<String?>(
        key = state.sharedElementKey,
    ) { imageUrl, innerModifier ->
        AsyncRasterImage(
            imageUrl = imageUrl,
            modifier = innerModifier
        )
    }

    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        state = scrollState
    ) {
        item {
            thumbnail(
                state.headerThumbnail,
                Modifier
                    .heightIn(max = 300.dp)
                    .aspectRatio(ratio = 16f / 9f)
                    .padding(horizontal = 16.dp)
                    .clip(MaterialTheme.shapes.medium),
            )
        }
        item {
            Chips(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                name = "Categories:",
                chipInfoList = state.descriptorChips<Descriptor.Category>(),
            )
        }

        item {
            Spacer(modifier = Modifier.padding(16.dp))
        }

        item(key = BODY_KEY) {
            NestedScrollTextContainer(
                modifier = Modifier
                    .fillParentMaxSize()
                    .padding(horizontal = 16.dp),
                canConsumeScrollEvents = bodyInViewport,
                onScrolled = scrollState::dispatchRawDelta,
            ) {
                val richTextState = rememberRichTextState()
                RichTextEditor(
                    state = richTextState,
                    readOnly = true,
                )
                LaunchedEffect(archive) {
                    archive?.let {
                        richTextState.setMarkdown(it.body)
                        richTextState.selection = TextRange.Zero
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.padding(16.dp))
        }

        item {
            Chips(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                name = "Tags:",
                chipInfoList = state.descriptorChips<Descriptor.Tag>(),
            )
        }

        item {
            Spacer(modifier = Modifier.padding(64.dp + navBarSizeDp))
        }
    }

    // Pop nav if this archive does not exist anymore
    val wasDeleted = state.wasDeleted
    LaunchedEffect(wasDeleted) {
        if (wasDeleted) actions(Action.Navigate { navState.pop() })
    }

    // If the user fully expands the secondary pane, pop this destination back to the list
    LaunchedEffect(state.hasSecondaryPanel, state.paneAnchor) {
        if (state.hasSecondaryPanel && state.paneAnchor == PaneAnchor.Full) {
            actions(Action.Navigate { navState.pop() })
        }
    }
}
