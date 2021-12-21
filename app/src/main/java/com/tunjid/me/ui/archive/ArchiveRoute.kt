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

package com.tunjid.me.ui.archive

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import coil.size.Scale.FILL
import com.tunjid.me.LocalAppDependencies
import com.tunjid.me.data.archive.Archive
import com.tunjid.me.data.archive.ArchiveContentFilter
import com.tunjid.me.data.archive.ArchiveKind.Articles
import com.tunjid.me.data.archive.ArchiveQuery
import com.tunjid.me.data.archive.User
import com.tunjid.me.globalui.UiState
import com.tunjid.me.nav.Route
import com.tunjid.me.nav.push
import com.tunjid.me.ui.InitialUiState
import com.tunjid.me.ui.archive.ArchiveItem.ContentFilter
import com.tunjid.me.ui.archive.ArchiveItem.Loading
import com.tunjid.me.ui.archive.ArchiveItem.Result
import com.tunjid.me.ui.archivedetail.ArchiveDetailRoute
import com.tunjid.me.ui.asNoOpStateFlowMutator
import com.tunjid.mutator.accept
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.scan
import kotlinx.datetime.Clock
import kotlin.math.abs
import kotlin.math.max

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

data class ArchiveRoute(val query: ArchiveQuery) : Route<ArchiveMutator> {
    override val id: String
        get() = query.toString()

    @Composable
    override fun Render() {
        ArchiveScreen(
            mutator = LocalAppDependencies.current.routeDependencies(this)
        )
    }
}

@Composable
@ExperimentalMaterialApi
private fun ArchiveScreen(mutator: ArchiveMutator) {
    val state by mutator.state.collectAsState()
    val query = state.route.query

    InitialUiState(
        UiState(
            toolbarShows = true,
            toolbarTitle = query.kind.name,
            showsBottomNav = true,
            statusBarColor = MaterialTheme.colors.primary.toArgb(),
        )
    )

    val items = state.items
    val listStateSummary = state.listStateSummary

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = listStateSummary.firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = listStateSummary.firstVisibleItemScrollOffset
    )


    LazyColumn(state = listState) {
        items(
            items = items,
            key = ArchiveItem::key,
            itemContent = { item ->
                when (item) {
                    Loading -> ProgressBar()
                    is Result -> ArchiveCard(archiveItem = item)
                    is ContentFilter -> ArchiveFilters(filter = item.filter)
                }
            }
        )
    }

    // Endless scrolling
    LaunchedEffect(listState, items) {
        snapshotFlow {
            ScrollState(
                scrollOffset = listState.firstVisibleItemScrollOffset,
                queryOffset = max(
                    (items.getOrNull(listState.firstVisibleItemIndex) as? Result)
                        ?.query
                        ?.offset
                        ?: 0,
                    (items.getOrNull(
                        listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    ) as? Result)
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
                mutator.accept(
                    Action.Fetch(
                        ArchiveQuery(
                            kind = query.kind,
                            temporalFilter = query.temporalFilter,
                            offset = it.queryOffset
                        )
                    )
                )
            }
    }

    // Initial load
    LaunchedEffect(query) {
        mutator.accept(Action.Fetch(query = query))
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
private fun ArchiveFilters(filter: ArchiveContentFilter) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = 1.dp,
    ) {
        Column(
            Modifier.padding(8.dp)
        ) {
            Chips(
                name = "Categories:",
                chips = filter.categories,
                color = MaterialTheme.colors.primaryVariant
            )
            Chips(
                name = "Tags:",
                chips = filter.tags,
                color = MaterialTheme.colors.secondary
            )
        }
    }
}

@Composable
private fun ProgressBar() {
    Box(
        modifier = Modifier
            .padding(vertical = 24.dp)
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.Center)
        )
    }
}

@Composable
@ExperimentalMaterialApi
private fun ArchiveCard(archiveItem: Result) {
    val navMutator = LocalAppDependencies.current.navMutator

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        onClick = {
            navMutator.accept { push(ArchiveDetailRoute(archive = archiveItem.archive)) }
        },
        content = {
            Column {
                ArchiveThumbnail(archiveItem)
                Spacer(Modifier.height(8.dp))
                ArchiveCategories(
                    categories = archiveItem.archive.categories,
                    published = archiveItem.prettyDate,
                    onCategoryClicked = { category ->
                        navMutator.accept {
                            push(archiveItem.query.routeFilteredByCategory(category))
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                ArchiveBlurb(archiveItem = archiveItem)
                Spacer(Modifier.height(8.dp))
            }
        }
    )
}

private fun ArchiveQuery.routeFilteredByCategory(category: String) = ArchiveRoute(
    query = copy(
        contentFilter = ArchiveContentFilter(
            categories = when (val filter =
                contentFilter) {
                null -> listOf(category)
                else -> filter.categories.plus(
                    category
                ).distinct()
            }
        )
    )
)

@Composable
private fun ArchiveThumbnail(archiveItem: Result) {
    Image(
        painter = rememberImagePainter(archiveItem.archive.thumbnail) {
            scale(FILL)
        },
        contentScale = ContentScale.Crop,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

@Composable
private fun ArchiveCategories(
    categories: List<String>,
    published: String,
    onCategoryClicked: (String) -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Chips(
            chips = categories,
            color = MaterialTheme.colors.primaryVariant,
            onClick = onCategoryClicked
        )
        Spacer(Modifier.weight(1f))
        Text(
            modifier = Modifier.wrapContentWidth(),
            text = published,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ArchiveBlurb(archiveItem: Result) {
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

private val sampleArchiveItem = Result(
    query = ArchiveQuery(kind = Articles),
    archive = Archive(
        key = "",
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

@Preview
@Composable
fun PreviewArchiveCard() {
    ArchiveCard(archiveItem = sampleArchiveItem)
}

@Preview
@Composable
fun PreviewLoadingState() {
    ArchiveScreen(
        mutator = State(
            route = ArchiveRoute(query = ArchiveQuery(kind = Articles)),
            activeQuery = ArchiveQuery(kind = Articles),
            items = listOf(Loading)
        ).asNoOpStateFlowMutator()
    )
}
