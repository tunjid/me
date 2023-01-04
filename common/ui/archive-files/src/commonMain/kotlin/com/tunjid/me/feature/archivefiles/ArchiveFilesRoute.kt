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

package com.tunjid.me.feature.archivefiles

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.ui.Thumbnail
import com.tunjid.me.feature.LocalScreenStateHolderCache
import com.tunjid.me.scaffold.lifecycle.toActionableState
import com.tunjid.me.scaffold.nav.AppRoute
import kotlinx.serialization.Serializable

@Serializable
data class ArchiveFilesRoute(
    override val id: String,
    val kind: ArchiveKind,
    val archiveId: ArchiveId,
) : AppRoute {
    @Composable
    override fun Render() {
        ArchiveFilesScreen(
            stateHolder = LocalScreenStateHolderCache.current.screenStateHolderFor(this),
        )
    }
}

@Composable
private fun ArchiveFilesScreen(
    stateHolder: ArchiveFilesStateHolder,
) {
    val screenUiState by stateHolder.toActionableState()
    val (state, actions) = screenUiState

    if (state.isMainContent) GlobalUi(
        state = state,
        onAction = actions
    )

    Column {
        Text("Upload items here")
        LazyVerticalGrid(
            columns = GridCells.Adaptive(100.dp)
        ) {
            items(
                items = state.files,
                key = { it.url },
                itemContent = {
                    Box(
                        modifier = Modifier.aspectRatio(1f)
                    ) {
                        Thumbnail(
                            imageUrl = it.url,
                            modifier = Modifier
                        )
                    }
                }
            )
        }
    }

}
