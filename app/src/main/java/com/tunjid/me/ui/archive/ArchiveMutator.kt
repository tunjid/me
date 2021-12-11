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
import kotlinx.coroutines.flow.map

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
): Mutator<Action, StateFlow<State>> = stateFlowMutator(
    scope = scope,
    initialState = State(),
    started = SharingStarted.WhileSubscribed(2000),
    transform = { actions ->
        actions.toMutationStream {
            when (val action = type()) {
                is Action.Fetch -> action.flow
                    .map { it.query }
                    .toArchives(repo = repo)
                    .map { archives ->
                        Mutation { copy(archives = archives) }
                    }
            }
        }
    }
)

private fun Flow<ArchiveQuery>.toArchives(repo: ArchiveRepository) =
    map { Tile.Request.On<ArchiveQuery, List<Archive>>(it) }
        .flattenWith(
            tiledList(
                flattener = Tile.Flattener.PivotSorted(
                    comparator = compareBy(ArchiveQuery::offset),
                ),
                fetcher = repo::archives
            )
        )
        .map { it.flatten() }
