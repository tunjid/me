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

package com.tunjid.me.common.ui.archive

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.me.common.AppAction
import com.tunjid.me.common.LocalAppDependencies
import com.tunjid.me.common.data.archive.Archive
import com.tunjid.me.common.data.archive.ArchiveKind.Articles
import com.tunjid.me.common.data.archive.ArchiveQuery
import com.tunjid.me.common.data.archive.Descriptor
import com.tunjid.me.common.data.archive.User
import com.tunjid.me.common.data.archive.plus
import com.tunjid.me.common.globalui.NavVisibility
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.nav.AppRoute
import com.tunjid.me.common.ui.InitialUiState
import com.tunjid.me.common.ui.archivedetail.ArchiveDetailRoute
import com.tunjid.me.common.ui.asNoOpStateFlowMutator
import com.tunjid.treenav.Route
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.takeWhile
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

data class ScrollState(
    val scrollOffset: Int = 0,
    val dy: Int = 0,
    val queryOffset: Int = 0,
    val isDownward: Boolean = true,
)

private fun ScrollState.updateDirection(new: ScrollState) = new.copy(
    queryOffset = new.queryOffset,
    dy = new.scrollOffset - scrollOffset,
    isDownward = when {
        abs(new.scrollOffset - scrollOffset) > 10 -> isDownward
        else -> new.scrollOffset > scrollOffset
    }
)

@Serializable
data class ArchiveRoute(val query: ArchiveQuery) : AppRoute<ArchiveMutator> {
    override val id: String
        get() = query.toString()

    @Composable
    override fun Render() {
        ArchiveScreen(
            mutator = LocalAppDependencies.current.routeDependencies(this),
        )
    }
}

@Composable
private fun ArchiveScreen(
    mutator: ArchiveMutator,
) {
    val state by mutator.state.collectAsState()
    val isInNavRail = state.isInNavRail
    val query = state.queryState.startQuery
    if (!isInNavRail) InitialUiState(
        UiState(
            toolbarShows = true,
            toolbarTitle = query.kind.name,
            navVisibility = NavVisibility.Visible,
            statusBarColor = MaterialTheme.colors.primary.toArgb(),
        )
    )

    val filter = state.queryState
    val chunkedItems = state.chunkedItems
    val listStateSummary = state.listStateSummary

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = listStateSummary.firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = listStateSummary.firstVisibleItemScrollOffset
    )

    Column {
        ArchiveFilters(
            item = filter,
            onChanged = mutator.accept
        )
        // TODO: Replace the chunking logic with a lazy grid once that has support for keys
        LazyColumn(state = listState) {
            items(
                items = chunkedItems,
                key = { it.first().key },
                itemContent = { chunk ->
                    ArchiveRow(
                        isInNavRail = isInNavRail,
                        items = chunk,
                        onAction = mutator.accept
                    )
                }
            )
        }
    }

    // Endless scrolling
    LaunchedEffect(listState, chunkedItems) {
        snapshotFlow {
            ScrollState(
                scrollOffset = listState.firstVisibleItemScrollOffset,
                queryOffset = max(
                    (chunkedItems.getOrNull(listState.firstVisibleItemIndex)
                        ?.lastOrNull() as? ArchiveItem.Result)
                        ?.query
                        ?.offset
                        ?: 0,
                    (chunkedItems.getOrNull(
                        listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    )?.lastOrNull() as? ArchiveItem.Result)
                        ?.query
                        ?.offset
                        ?: 0
                )
            )
        }
            .scan(ScrollState(), ScrollState::updateDirection)
            .filter { abs(it.dy) > 4 }
            .distinctUntilChangedBy(ScrollState::queryOffset)
            .collect {
                mutator.accept(Action.UserScrolled)
                mutator.accept(Action.ToggleFilter(isExpanded = false))
                mutator.accept(
                    Action.Fetch(
                        ArchiveQuery(
                            kind = query.kind,
                            temporalFilter = query.temporalFilter,
                            contentFilter = state.queryState.startQuery.contentFilter,
                            offset = it.queryOffset
                        )
                    )
                )
            }
    }

    // Initial load
    LaunchedEffect(query) {
        mutator.accept(Action.Fetch(query = state.queryState.currentQuery))
    }

    // Data is loaded in chunks, in case the lower section loads before the upper section
    // scroll to the upper section
    val size = state.items.size
    val shouldScrollToTop = state.shouldScrollToTop
    val startScrollPosition = state.listStateSummary.firstVisibleItemIndex

    LaunchedEffect(shouldScrollToTop, startScrollPosition, size) {
        snapshotFlow { Triple(shouldScrollToTop, startScrollPosition, size) }
            .distinctUntilChangedBy { it.third }
            .takeWhile { it.first }
            .collect {
                listState.scrollToItem(it.second)
            }
    }

    // Scroll state preservation
    DisposableEffect(query) {
        onDispose {
            mutator.accept(
                Action.UpdateListState(
                    ListState(
                        firstVisibleItemIndex = listState.firstVisibleItemIndex,
                        firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                    )
                )
            )
        }
    }
}

@Composable
private fun ProgressBar(isCircular: Boolean) {
    Box(
        modifier = Modifier
            .padding(vertical = 24.dp)
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        if (isCircular) CircularProgressIndicator(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.Center),
            color = MaterialTheme.colors.onSurface
        )
        else LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .align(Alignment.Center),
            color = MaterialTheme.colors.onSurface
        )
    }
}

@Composable
private fun ArchiveRow(
    isInNavRail: Boolean,
    items: List<ArchiveItem>,
    onAction: (Action) -> Unit
) {
    val minCardWidthPx = with(LocalDensity.current) {
        350.dp.toPx()
    }
    Row(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .onGloballyPositioned {
                onAction(
                    Action.GridSize(
                        size = max(1, (it.size.width / minCardWidthPx).roundToInt())
                    )
                )
            }
    ) {
        items.forEach { item ->
            when (item) {
                is ArchiveItem.Loading -> ProgressBar(isCircular = item.isCircular)
                is ArchiveItem.Result -> ArchiveCard(
                    archiveItem = item,
                    onAction = onAction,
                    onNavAction = { route ->
                        onAction(
                            Action.Navigate(
                                when {
                                    isInNavRail -> AppAction.Nav.swap(route)
                                    else -> AppAction.Nav.push(route)
                                }
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun RowScope.ArchiveCard(
    archiveItem: ArchiveItem.Result,
    onAction: (Action) -> Unit,
    onNavAction: (Route) -> Unit
) {
    Card(
        modifier = Modifier
            .weight(1F)
            .padding(16.dp),
        onClick = {
            onNavAction(
                ArchiveDetailRoute(
                    kind = archiveItem.archive.kind,
                    archiveId = archiveItem.archive.id
                )
            )
        },
        content = {
            Column {
                ArchiveThumbnail(archiveItem.archive.thumbnail)
                Spacer(Modifier.height(8.dp))
                ArchiveCategories(
                    categories = archiveItem.archive.categories,
                    published = archiveItem.prettyDate,
                    onCategoryClicked = { category ->
                        val query = archiveItem.query
                        onAction(
                            Action.Fetch(
                                query = query.copy(offset = 0) + Descriptor.Category(category),
                                reset = true
                            )
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
                ArchiveBlurb(archiveItem = archiveItem)
                Spacer(Modifier.height(8.dp))
            }
        }
    )
}

@Composable
private fun ArchiveThumbnail(imageUrl: String?) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
    val painter = archivePainter(imageUrl)

    if (painter != null) Image(
        painter = painter,
        contentScale = ContentScale.Crop,
        contentDescription = null,
        modifier = modifier
    )
    else Box(modifier = modifier)
}

@Composable
private fun ArchiveCategories(
    categories: List<String>,
    published: String,
    onCategoryClicked: (String) -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Chips(
            modifier = Modifier.weight(1F),
            chips = categories,
            color = MaterialTheme.colors.primaryVariant,
            onClick = onCategoryClicked
        )
        Text(
            modifier = Modifier.wrapContentWidth(),
            text = published,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ArchiveBlurb(archiveItem: ArchiveItem.Result) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = archiveItem.archive.title,
            fontSize = 18.sp,
        )
        Spacer(modifier = Modifier.padding(vertical = 2.dp))
        Text(
            text = archiveItem.archive.description,
            fontSize = 15.sp,
        )
    }
}

private val sampleArchiveItem = ArchiveItem.Result(
    query = ArchiveQuery(kind = Articles),
    archive = Archive(
        id = "",
        link = "https://storage.googleapis.com/tunji-web-public/article-media/1P372On2TSH-rAuBsbWLGSQ.jpeg",
        title = "I'm an Archive",
        body = "Hello",
        description = "Hi",
        thumbnail = "https://storage.googleapis.com/tunji-web-public/article-media/1P372On2TSH-rAuBsbWLGSQ.jpeg",
        author = User(
            id = "i",
            firstName = "TJ",
            lastName = "D",
            fullName = "TJ D",
            imageUrl = "",
        ),
        created = Clock.System.now(),
        tags = listOf(),
        categories = listOf("Android", "Kotlin"),
        kind = Articles,
    )
)

@Composable
expect fun archivePainter(imageUrl: String?): Painter?

//@Preview
@Composable
private fun PreviewArchiveCard() {
    ArchiveRow(
        isInNavRail = false,
        items = listOf(sampleArchiveItem),
        onAction = { }
    )
}

//@Preview
@Composable
private fun PreviewLoadingState() {
    ArchiveScreen(
        mutator = State(
            queryState = QueryState(
                startQuery = ArchiveQuery(kind = Articles),
                currentQuery = ArchiveQuery(kind = Articles),
            ),
            items = listOf(
                ArchiveItem.Loading(
                    isCircular = true,
                    query = ArchiveQuery(kind = Articles)
                )
            )
        ).asNoOpStateFlowMutator()
    )
}
