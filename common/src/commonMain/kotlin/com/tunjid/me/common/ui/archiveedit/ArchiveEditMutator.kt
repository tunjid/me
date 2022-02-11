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

package com.tunjid.me.common.ui.archiveedit


import com.tunjid.me.common.app.AppMutator
import com.tunjid.me.common.data.ByteSerializable
import com.tunjid.me.common.data.model.Archive
import com.tunjid.me.common.data.repository.ArchiveRepository
import com.tunjid.me.common.globalui.navBarSize
import com.tunjid.me.common.app.monitorWhenActive
import com.tunjid.me.common.data.repository.AuthRepository
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.accept
import com.tunjid.mutator.coroutines.stateFlowMutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

typealias ArchiveEditMutator = Mutator<Unit, StateFlow<State>>

fun archiveEditMutator(
    scope: CoroutineScope,
    route: ArchiveEditRoute,
    initialState: State? = null,
    archiveRepository: ArchiveRepository,
    authRepository: AuthRepository,
    appMutator: AppMutator,
): ArchiveEditMutator = stateFlowMutator(
    scope = scope,
    initialState = initialState ?: State(
        navBarSize = appMutator.globalUiMutator.state.value.navBarSize,
    ),
    started = SharingStarted.WhileSubscribed(2000),
    actionTransform = {
        merge<Mutation<State>>(
            appMutator.globalUiMutator.state
                .map { it.navBarSize }
                .map {
                    Mutation { copy(navBarSize = it) }
                },
            authRepository.isSignedIn.map { Mutation { copy(isSignedIn = it) } },
            archiveRepository.monitorArchive(
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
