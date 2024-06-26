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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.tunjid.me.archivedetail.di.kind
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.ui.AsyncRasterImage
import com.tunjid.me.core.ui.Chips
import com.tunjid.me.core.ui.MediaArgs
import com.tunjid.me.core.ui.NestedScrollTextContainer
import com.tunjid.me.core.ui.isInViewport
import com.tunjid.me.scaffold.globalui.PaneAnchor
import com.tunjid.me.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.scaffold.adaptive.routeOf
import com.tunjid.scaffold.adaptive.sharedElementOf
import com.tunjid.treenav.strings.RouteParams

private const val BODY_KEY = 3

fun ArchiveDetailRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
    children = listOf(
        routeOf(
            path = "/archives/${routeParams.kind.type}",
            pathArgs = mapOf(
                "kind" to routeParams.kind.type
            )
        )
    )
)

@Composable
internal fun ArchiveDetailScreen(
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
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

    val thumbnail = sharedElementOf<MediaArgs>(
        key = state.sharedElementKey,
    ) { args, innerModifier ->
        AsyncRasterImage(
            args = args,
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
                MediaArgs(
                    url = state.headerThumbnail,
                    contentScale = ContentScale.Crop,
                ),
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
        if (wasDeleted) actions(Action.Navigate.Pop)
    }

    // If the user fully expands the secondary pane, pop this destination back to the list
    LaunchedEffect(state.hasSecondaryPanel, state.paneAnchor) {
        if (state.hasSecondaryPanel && state.paneAnchor == PaneAnchor.Full) {
            actions(Action.Navigate.Pop)
        }
    }
}
