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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.toArgb
import com.tunjid.me.common.LocalAppDependencies
import com.tunjid.me.common.data.archive.ArchiveKind
import com.tunjid.me.common.data.archive.ArchiveQuery
import com.tunjid.me.common.globalui.NavVisibility
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.nav.AppRoute
import com.tunjid.me.common.ui.utilities.InitialUiState
import com.tunjid.mutator.coroutines.asNoOpStateFlowMutator
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.scan
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max

private data class ScrollState(
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
        get() = "archive-route-${query.kind}"

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
    val listState = rememberLazyListState()

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
                    Action.Fetch.LoadMore(
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
        mutator.accept(Action.Fetch.LoadMore(query = state.queryState.currentQuery))
    }
}

//@Preview
@Composable
private fun PreviewLoadingState() {
    ArchiveScreen(
        mutator = State(
            queryState = QueryState(
                startQuery = ArchiveQuery(kind = ArchiveKind.Articles),
                currentQuery = ArchiveQuery(kind = ArchiveKind.Articles),
            ),
            items = listOf(
                ArchiveItem.Loading(
                    isCircular = true,
                    query = ArchiveQuery(kind = ArchiveKind.Articles)
                )
            )
        ).asNoOpStateFlowMutator()
    )
}
