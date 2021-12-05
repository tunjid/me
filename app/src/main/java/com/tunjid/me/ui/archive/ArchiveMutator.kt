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

import com.tunjid.me.data.Api
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class State(
    val archives: List<Archive> = listOf()
)

data class ArchiveQuery(
    val year: Int?,
    val month: Int?
)

sealed class Action {
    data class Load(
        val kind: ArchiveKind,
        val before: ArchiveQuery,
        val offset: Int,
        val limit: Int = 20
    ) : Action()
}

fun archiveMutator(
    scope: CoroutineScope,
    api: Api
): Mutator<Action, StateFlow<State>> = stateFlowMutator(
    scope = scope,
    initialState = State(),
    started = SharingStarted.WhileSubscribed(2000),
    transform = { actions ->
        actions.toMutationStream {
            when (val action = type()) {
                is Action.Load -> action.flow
                    .toArchives(api = api)
                    .map { archives ->
                        Mutation { copy(archives = archives) }
                    }
            }
        }
    }
)

private fun Flow<Action.Load>.toArchives(api: Api) =
    map { Tile.Request.On<Action.Load, List<Archive>>(it) }
        .flattenWith(tiledList(
            flattener = Tile.Flattener.PivotSorted(
                comparator = compareBy(Action.Load::offset),
            ),
            fetcher = { load ->
                flowOf(
                    api.fetchArchives(
                        kind = load.kind,
                        options = mapOf(
                            "offset" to load.offset.toString()
                        )
                    )
                )
            }
        ))
        .map { it.flatten() }
