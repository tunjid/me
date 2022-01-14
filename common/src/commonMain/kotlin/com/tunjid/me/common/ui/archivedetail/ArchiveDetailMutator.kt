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


import com.tunjid.me.common.AppMutator
import com.tunjid.me.common.data.archive.Archive
import com.tunjid.me.common.data.archive.ArchiveKind
import com.tunjid.me.common.data.archive.ArchiveRepository
import com.tunjid.me.common.globalui.navBarSize
import com.tunjid.me.common.monitorWhenActive
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.accept
import com.tunjid.mutator.coroutines.stateFlowMutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

data class State(
    val navBarSize: Int,
    val archive: Archive? = null,
)

typealias ArchiveDetailMutator = Mutator<Unit, StateFlow<State>>

fun archiveDetailMutator(
    scope: CoroutineScope,
    route: ArchiveDetailRoute,
    repo: ArchiveRepository,
    appMutator: AppMutator,
): ArchiveDetailMutator = stateFlowMutator(
    scope = scope,
    initialState = State(
        navBarSize = appMutator.globalUiMutator.state.value.navBarSize,
    ),
    started = SharingStarted.WhileSubscribed(2000),
    transform = {
        merge<Mutation<State>>(
            appMutator.globalUiMutator.state
                .map { it.navBarSize }
                .map {
                    Mutation { copy(navBarSize = it) }
                },
            repo.monitorArchive(
                kind = route.kind,
                id = route.archiveId
            )
                .map { fetchedArchive ->
                    appMutator.globalUiMutator.accept {
                        copy(toolbarTitle = fetchedArchive.title)
                    }
                    Mutation { copy(archive = fetchedArchive) }
                }
        ).monitorWhenActive(appMutator)
    }
)
