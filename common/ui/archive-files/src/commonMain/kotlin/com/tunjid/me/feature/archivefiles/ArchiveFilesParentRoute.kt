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

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.feature.LocalScreenStateHolderCache
import com.tunjid.me.scaffold.globalui.NavVisibility
import com.tunjid.me.scaffold.globalui.ScreenUiState
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.nav.StatelessRoute
import com.tunjid.treenav.Node
import kotlinx.serialization.Serializable

@Serializable
data class ArchiveFilesParentRoute(
    override val id: String,
    val kind: ArchiveKind,
    val archiveId: ArchiveId,
) : AppRoute, StatelessRoute {

    override val children: List<Node> = FileType.values()
        .map { fileType ->
            ArchiveFilesRoute(
                id = "$id?type=${fileType.name.lowercase()}",
                kind = kind,
                archiveId = archiveId,
                dndEnabled = false,
                fileType = fileType
            )
        }

    @Composable
    override fun Render() {
        ScreenUiState(
            UiState(
                toolbarShows = true,
                toolbarTitle = "Files",
                fabShows = false,
                fabExtended = true,
                navVisibility = NavVisibility.Visible,
                statusBarColor = MaterialTheme.colorScheme.surface.toArgb(),
            )
        )
        ArchiveFilesParentScreen(
            children = children.filterIsInstance<ArchiveFilesRoute>(),
        )
    }
}

@Composable
private fun ArchiveFilesParentScreen(
    children: List<ArchiveFilesRoute>
) {
    val pagerState = rememberPagerState()
    var lastTabClicked by remember { mutableStateOf<Int?>(pagerState.currentPage) }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Tabs(
            currentTabIndex = pagerState.currentPage,
            titles = children.map { it.fileType.name },
            onTabClicked = { lastTabClicked = it }
        )
        HorizontalPager(
            pageCount = children.size,
            state = pagerState,
        ) { index ->
            ArchiveFilesScreen(
                stateHolder = LocalScreenStateHolderCache.current
                    .screenStateHolderFor(children[index])
            )
        }
    }

    LaunchedEffect(lastTabClicked) {
        lastTabClicked?.let { pagerState.animateScrollToPage(it) }
    }
}

@Composable
private fun Tabs(
    currentTabIndex: Int,
    titles: List<String>,
    onTabClicked: (Int) -> Unit
) {
    var offset by remember { mutableStateOf(0.dp) }
    val animatedOffset by animateDpAsState(offset)
    val widthPercent = 1f / titles.size
    BoxWithConstraints {
        Column {
            Row {
                titles.forEachIndexed { index, title ->
                    Tab(
                        modifier = Modifier.weight(1f),
                        selected = index == currentTabIndex,
                        onClick = { onTabClicked(index) },
                        text = { Text(text = title) }
                    )
                }
            }
            Box(
                modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth(widthPercent)
                    .offset(x = animatedOffset)
                    .background(MaterialTheme.colorScheme.onSurface)
            )
        }
        offset = maxWidth * widthPercent * currentTabIndex
    }
}