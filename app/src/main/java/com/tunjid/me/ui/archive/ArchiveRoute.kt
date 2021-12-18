/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.me.ui.archive

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import coil.size.Scale
import com.tunjid.me.LocalAppDependencies
import com.tunjid.me.data.archive.Archive
import com.tunjid.me.data.archive.ArchiveKind
import com.tunjid.me.data.archive.ArchiveQuery
import com.tunjid.me.data.archive.User
import com.tunjid.me.globalui.UiState
import com.tunjid.me.nav.Route
import com.tunjid.me.nav.push
import com.tunjid.me.ui.InitialUiState
import com.tunjid.me.ui.archivedetail.ArchiveDetailRoute
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
    @ExperimentalMaterialApi
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
            toolbarTitle = query.kind.type,
            showsBottomNav = true
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
            itemContent = { ArchiveCard(it) }
        )
    }

    LaunchedEffect(listState, items) {
        snapshotFlow {
            ScrollState(
                scrollOffset = listState.firstVisibleItemScrollOffset,
                queryOffset = max(
                    items.getOrNull(listState.firstVisibleItemIndex)
                        ?.query
                        ?.offset
                        ?: 0,
                    items.getOrNull(
                        listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    )
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
                            filter = query.filter,
                            offset = it.queryOffset
                        )
                    )
                )
            }
    }

    LaunchedEffect(query.kind) {
        println("In ${query.kind}; s: ${state.items.size}; l: ${state.listStateSummary}")

        mutator.accept(Action.Fetch(query = query))
    }

    DisposableEffect(query.kind) {
        onDispose {
            println("ybyvb")
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
@ExperimentalMaterialApi
private fun ArchiveCard(archiveItem: ArchiveItem) {
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
                Image(
                    painter = rememberImagePainter(archiveItem.archive.thumbnail) {
                        scale(Scale.FILL)
                    },
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                Spacer(Modifier.height(8.dp))
                ArchiveTags(
                    categories = archiveItem.archive.categories,
                    published = archiveItem.prettyDate
                )
                Spacer(Modifier.height(8.dp))
                ArchiveBlurb(archiveItem = archiveItem)
                Spacer(Modifier.height(8.dp))
            }
        }
    )
}

@Composable
private fun ArchiveTags(categories: List<String>, published: String) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            categories.forEach { tag ->
                Button(
                    modifier = Modifier
                        .wrapContentSize()
                        .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
                    onClick = {},
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
        }
        Spacer(Modifier.weight(1f))

        Text(
            modifier = Modifier.wrapContentWidth(),
            text = published.toString(),
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ArchiveBlurb(archiveItem: ArchiveItem) {
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

private val sampleArchiveItem = ArchiveItem(
    query = ArchiveQuery(kind = ArchiveKind.Articles),
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
        kind = ArchiveKind.Articles,
    )
)

@Preview
@Composable
@ExperimentalMaterialApi
fun Test() {
    ArchiveCard(archiveItem = sampleArchiveItem)
}
