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

package com.tunjid.me.common.ui.archivedetail


import com.tunjid.me.common.data.archive.Archive
import com.tunjid.me.common.data.archive.ArchiveRepository
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

data class State(
    val archive: Archive,
)

typealias ArchiveDetailMutator = Mutator<Unit, StateFlow<State>>

fun archiveDetailMutator(
    scope: CoroutineScope,
    archive: Archive,
    repo: ArchiveRepository
): ArchiveDetailMutator = stateFlowMutator(
    scope = scope,
    initialState = State(archive = archive),
    started = SharingStarted.WhileSubscribed(2000),
    transform = {
        repo.monitorArchive(
            kind = archive.kind,
            id = archive.key
        )
            .map { fetchedArchive ->
                Mutation { copy(archive = fetchedArchive) }
            }
    }
)
