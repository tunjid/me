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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.unit.dp
import com.tunjid.me.archivedetail.di.kind
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.ui.AsyncRasterImage
import com.tunjid.me.core.ui.Chips
import com.tunjid.me.core.ui.MediaArgs
import com.tunjid.me.scaffold.adaptive.routeOf
import com.tunjid.me.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import com.tunjid.treenav.strings.RouteParams

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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ArchiveDetailScreen(
    movableSharedElementScope: MovableSharedElementScope,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) = BlogMarkdown(
    modifier = modifier,
    markdown = state.archive?.body ?: "",
    content = { markdownState, components, innerModifier ->
        val scrollState = rememberLazyListState()

        // Close the secondary pane when invoking back since it contains the list view
        SecondaryPaneCloseBackHandler(
            enabled = state.isInPrimaryNav && state.hasSecondaryPanel
        )

        LazyColumn(
            modifier = innerModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            state = scrollState
        ) {
            item {
                movableSharedElementScope.updatedMovableSharedElementOf(
                    key = state.sharedElementKey,
                    MediaArgs(
                        url = state.headerThumbnail,
                        contentScale = ContentScale.Crop,
                    ),
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .aspectRatio(ratio = 16f / 9f)
                        .padding(horizontal = 16.dp)
                        .clip(MaterialTheme.shapes.medium),
                    sharedElement = { state, innerModifier ->
                        AsyncRasterImage(
                            args = state,
                            modifier = innerModifier
                        )
                    }
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

            blogItems(
                innerMarkdownState = markdownState,
                components = components,
            )

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
                Spacer(
                    modifier = Modifier
                        .padding(64.dp)
                        .navigationBarsPadding()
                )
            }
        }

        // Pop nav if this archive does not exist anymore
        val wasDeleted = state.wasDeleted
        LaunchedEffect(wasDeleted) {
            if (wasDeleted) actions(Action.Navigate.Pop)
        }
    }
)
