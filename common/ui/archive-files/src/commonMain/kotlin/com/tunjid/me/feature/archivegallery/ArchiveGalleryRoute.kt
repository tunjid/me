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

package com.tunjid.me.feature.archivegallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.tunjid.me.core.model.ArchiveFileId
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.ui.rememberAsyncRasterPainter
import com.tunjid.me.feature.rememberRetainedStateHolder
import com.tunjid.me.scaffold.globalui.adaptive.Adaptive
import com.tunjid.me.scaffold.globalui.adaptive.rememberSharedContent
import com.tunjid.me.scaffold.globalui.adaptive.thumbnailSharedElementKey
import com.tunjid.me.scaffold.lifecycle.component1
import com.tunjid.me.scaffold.lifecycle.component2
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.tiler.compose.PivotedTilingEffect
import kotlinx.serialization.Serializable


@Serializable
data class ArchiveGalleryRoute(
    override val id: String,
    val archiveId: ArchiveId,
    val pageOffset: Int = 0,
    val archiveFileIds: List<ArchiveFileId> = emptyList(),
    val urls: List<String> = emptyList(),
) : AppRoute {
    override val content: @Composable Adaptive.ContainerScope.() -> Unit
        get() = {
            ArchiveGalleryScreen(
                stateHolder = rememberRetainedStateHolder(
                    route = this@ArchiveGalleryRoute
                ),
            )
        }
}

@Composable
internal fun ArchiveGalleryScreen(
    stateHolder: ArchiveGalleryStateHolder,
) {
    GlobalUi()

    val (state, actions) = stateHolder
    val items by rememberUpdatedState(state.items)

    val pagerState = rememberPagerState { items.size }

    HorizontalPager(
        state = pagerState,
        beyondBoundsPageCount = 0,
        modifier = Modifier.fillMaxSize(),
        key = { index -> items[index].key }
    ) { index ->
        val file = items[index]
        val sharedElement = rememberSharedContent(
            thumbnailSharedElementKey(file.archiveFileId)
        ) { modifier ->
            val imagePainter = rememberAsyncRasterPainter(
                imageUri = file.url,
            )
            if (imagePainter != null) Image(
                modifier = modifier,
                painter = imagePainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
        sharedElement(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )
    }

    pagerState.PivotedTilingEffect(
        items = items,
        onQueryChanged = {
            actions(Action.LoadAround(query = it ?: state.currentQuery))
        }
    )
}
