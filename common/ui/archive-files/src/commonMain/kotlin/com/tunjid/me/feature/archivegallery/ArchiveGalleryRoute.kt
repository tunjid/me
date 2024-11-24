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

// See YouTrack: KTIJ-18375
@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package com.tunjid.me.feature.archivegallery

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.tunjid.me.core.ui.AsyncRasterImage
import com.tunjid.me.core.ui.MediaArgs
import com.tunjid.me.scaffold.adaptive.routeOf
import com.tunjid.me.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.me.scaffold.scaffold.dragToPop
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import com.tunjid.treenav.strings.RouteParams

fun ArchiveGalleryRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ArchiveGalleryScreen(
    movableSharedElementScope: MovableSharedElementScope,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items by rememberUpdatedState(state.items)
    val pagerState = rememberPagerState { items.size }

    HorizontalPager(
        state = pagerState,
//        beyondBoundsPageCount = 0,
        modifier = modifier
            .dragToPop()
            .fillMaxSize(),
        key = { index -> items[index].key }
    ) { index ->
        val file = items[index]
        movableSharedElementScope.updatedMovableSharedElementOf(
            key = thumbnailSharedElementKey(file.url),
            state = MediaArgs(
                url = file.url,
                contentScale = ContentScale.Crop
            ),
            modifier = Modifier.fillMaxSize(),
            sharedElement = { state, innerModifier ->
                AsyncRasterImage(
                    args = state,
                    modifier = innerModifier
                )
            }
        )
    }

    pagerState.PivotedTilingEffect(
        items = items,
        onQueryChanged = {
            actions(Action.LoadAround(query = it ?: state.currentQuery))
        }
    )
}
