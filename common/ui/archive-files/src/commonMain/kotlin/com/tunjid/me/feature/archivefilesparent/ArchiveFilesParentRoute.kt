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

package com.tunjid.me.feature.archivefilesparent

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.me.feature.archivefiles.ActualArchiveFilesStateHolder
import com.tunjid.me.feature.archivefiles.ArchiveFilesScreen
import com.tunjid.me.feature.archivefiles.FileType
import com.tunjid.me.feature.archivefiles.di.fileType
import com.tunjid.me.feature.archivefilesparent.di.archiveId
import com.tunjid.me.feature.archivefilesparent.di.kind
import com.tunjid.me.scaffold.adaptive.routeOf
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.strings.RouteParams

fun ArchiveFilesParentRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
    children = FileType.entries
        .map { fileType ->
            routeOf(
                params = RouteParams(
                    pathAndQueries = "/archives/${routeParams.kind.type}/${routeParams.archiveId.value}/files/${fileType.kind}",
                    pathArgs = routeParams.pathArgs + ("type" to fileType.kind),
                    queryParams = routeParams.queryParams
                ),
            )
        }
)

@Composable
internal fun ArchiveFilesParentScreen(
    movableSharedElementScope: MovableSharedElementScope,
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit,
) {
    val pagerState = rememberPagerState { state.children.size }
    var lastTabClicked by remember { mutableStateOf<Int?>(pagerState.currentPage) }
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars),
            navigationIcon = {
                IconButton(
                    modifier = Modifier
                        .size(56.dp)
                        .padding(16.dp),
                    onClick = {
                        actions(Action.Navigate.Pop)
                    },
                    content = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                )
            },
            title = {
                Text(
                    text = "Files",
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                )
            },
        )
        Tabs(
            currentTabIndex = pagerState.currentPage,
            titles = state.children.map { it.routeParams.fileType.name },
            onTabClicked = { lastTabClicked = it }
        )
        HorizontalPager(
            state = pagerState,
        ) { index ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            state.childCreator?.let { creator ->
                val viewModel = viewModel<ActualArchiveFilesStateHolder> {
                    creator.invoke(
                        scope = lifecycleCoroutineScope,
                        route = state.children[index],
                    )
                }
                ArchiveFilesScreen(
                    movableSharedElementScope = movableSharedElementScope,
                    state = viewModel.state.collectAsStateWithLifecycle().value,
                    actions = viewModel.accept,
                )
            }
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