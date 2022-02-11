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
import com.tunjid.me.common.app.monitorWhenActive
import com.tunjid.me.common.data.model.ArchiveKind
import com.tunjid.me.common.data.model.Descriptor
import com.tunjid.me.common.data.repository.ArchiveRepository
import com.tunjid.me.common.data.repository.AuthRepository
import com.tunjid.me.common.globalui.navBarSize
import com.tunjid.me.common.ui.common.ChipAction
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import com.tunjid.mutator.coroutines.toMutationStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

typealias ArchiveEditMutator = Mutator<Action, StateFlow<State>>

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
        archiveId = route.archiveId,
        navBarSize = appMutator.globalUiMutator.state.value.navBarSize,
    ),
    started = SharingStarted.WhileSubscribed(2000),
    actionTransform = { actions ->
        merge(
            appMutator.globalUiMutator.state
                .map { it.navBarSize }
                .map { Mutation { copy(navBarSize = it) } },
            authRepository.isSignedIn.map { Mutation { copy(isSignedIn = it) } },
            route.archiveId?.let {
                archiveRepository.textBodyMutations(
                    kind = route.kind,
                    archiveId = it
                )
            } ?: emptyFlow(),
            actions.toMutationStream(keySelector = Action::key) {
                when (val action = type()) {
                    is Action.TextEdit -> action.flow.textEditMutations()
                    is Action.ChipEdit -> action.flow.chipEditMutations()
                }
            }
        ).monitorWhenActive(appMutator)
    },
)

private fun ArchiveRepository.textBodyMutations(
    kind: ArchiveKind,
    archiveId: String
): Flow<Mutation<State>> = monitorArchive(
    kind = kind,
    id = archiveId
).map { archive ->
    Mutation {
        copy(
            title = archive.title,
            description = archive.description,
            body = archive.body,
            chipsState = ChipsState(
                categories = archive.categories.map(Descriptor::Category),
                tags = archive.tags.map(Descriptor::Tag),
            )
        )
    }
}

private fun Flow<Action.TextEdit>.textEditMutations(): Flow<Mutation<State>> =
    map { it.mutation }

private fun Flow<Action.ChipEdit>.chipEditMutations(): Flow<Mutation<State>> =
    map { (chipAction, descriptor) ->
        Mutation {
            if (descriptor.value.isBlank()) return@Mutation this

            val updatedChipsState = when (chipAction) {
                ChipAction.Added -> chipsState.copy(
                    categories = when (descriptor) {
                        is Descriptor.Category -> (chipsState.categories + descriptor).distinct()
                        else -> chipsState.categories
                    },
                    categoryText = when (descriptor) {
                        is Descriptor.Category -> Descriptor.Category(value = "")
                        else -> chipsState.categoryText
                    },
                    tags = when (descriptor) {
                        is Descriptor.Tag -> (chipsState.tags + descriptor).distinct()
                        else -> chipsState.tags
                    },
                    tagText = when (descriptor) {
                        is Descriptor.Tag -> Descriptor.Tag(value = "")
                        else -> chipsState.tagText
                    },
                )
                is ChipAction.Changed -> chipsState.copy(
                    categoryText = when (descriptor) {
                        is Descriptor.Category -> descriptor
                        else -> chipsState.categoryText
                    },
                    tagText = when (descriptor) {
                        is Descriptor.Tag -> descriptor
                        else -> chipsState.tagText
                    }
                )
                is ChipAction.Removed -> chipsState.copy(
                    categories = chipsState.categories.filter { it != descriptor },
                    tags = chipsState.tags.filter { it != descriptor },
                )
            }
            copy(chipsState = updatedChipsState)
        }
    }