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

import com.tunjid.me.data.archive.Archive
import com.tunjid.me.data.archive.ArchiveQuery
import com.tunjid.me.data.archive.ArchiveRepository
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.Tile
import com.tunjid.tiler.flattenWith
import com.tunjid.tiler.tiledList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

typealias ArchiveMutator = Mutator<Action, StateFlow<State>>

data class State(
    val archives: List<Archive> = listOf()
)

sealed class Action {
    data class Fetch(val query: ArchiveQuery) : Action()
}

fun archiveMutator(
    scope: CoroutineScope,
    repo: ArchiveRepository
): Mutator<Action, StateFlow<State>> = stateFlowMutator<Action, State>(
    scope = scope,
    initialState = State(),
    started = SharingStarted.WhileSubscribed(2000),
    transform = { actions ->
        actions.toMutationStream {
            when (val action = type()) {
                is Action.Fetch -> action.flow
                    .toArchives(repo = repo)
                    .map { archives ->
                        Mutation { copy(archives = archives) }
                    }
            }
        }
    }
)

private fun ArchiveRepository.archiveTiler() = tiledList(
    flattener = Tile.Flattener.PivotSorted(
        comparator = compareBy(ArchiveQuery::offset),
    ),
    fetcher = this::archives
)

private fun Flow<Action.Fetch>.toArchives(repo: ArchiveRepository): Flow<List<Archive>> =
    queryChanges()
        .flatMapLatest { (oldPages, newPages) ->
            oldPages
                .filterNot { newPages.contains(it) }
                .map { Tile.Request.Off<ArchiveQuery, List<Archive>>(it) }
                .plus(newPages.map { Tile.Request.On(it) })
                .asFlow()
        }
        .flattenWith(repo.archiveTiler())
        .map { it.flatten() }

private fun Flow<Action.Fetch>.queryChanges(): Flow<Pair<List<ArchiveQuery>, List<ArchiveQuery>>> =
    map { (query) ->
        listOf(
            query.copy(offset = query.offset - query.limit),
            query.copy(offset = query.offset + query.limit),
            query
        )
            .filter { it.offset >= 0 }
    }
        .scan(Pair(listOf(), listOf())) { pair, new ->
            pair.copy(
                first = pair.second,
                second = new
            )
        }