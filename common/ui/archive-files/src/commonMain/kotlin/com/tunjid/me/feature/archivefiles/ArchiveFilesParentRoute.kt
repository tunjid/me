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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.tunjid.me.feature.archivefiles.di.archiveId
import com.tunjid.me.feature.archivefiles.di.fileType
import com.tunjid.me.feature.archivefiles.di.kind
import com.tunjid.me.scaffold.adaptive.routeOf
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.strings.Route
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
    creator: ArchiveFilesStateHolderCreator,
    children: List<Route>
) {
    val pagerState = rememberPagerState { children.size }
    var lastTabClicked by remember { mutableStateOf<Int?>(pagerState.currentPage) }
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Tabs(
            currentTabIndex = pagerState.currentPage,
            titles = children.map { it.routeParams.fileType.name },
            onTabClicked = { lastTabClicked = it }
        )
        HorizontalPager(
            state = pagerState,
        ) { index ->
            val stateHolder = creator.invoke(
                LocalLifecycleOwner.current.lifecycleScope,
                children[index],
            )
            ArchiveFilesScreen(
                movableSharedElementScope = movableSharedElementScope,
                state = stateHolder.state.collectAsStateWithLifecycle().value,
                actions = stateHolder.accept,
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