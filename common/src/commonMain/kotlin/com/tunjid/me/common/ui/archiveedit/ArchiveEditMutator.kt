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
import com.tunjid.me.common.data.model.*
import com.tunjid.me.common.data.repository.ArchiveRepository
import com.tunjid.me.common.data.repository.AuthRepository
import com.tunjid.me.common.globalui.navBarSize
import com.tunjid.me.common.globalui.navBarSizeMutations
import com.tunjid.me.common.ui.common.ChipAction
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import com.tunjid.mutator.coroutines.toMutationStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

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
        kind = route.kind,
        upsert = ArchiveUpsert(id = route.archiveId),
        navBarSize = appMutator.globalUiMutator.state.value.navBarSize,
    ),
    started = SharingStarted.WhileSubscribed(2000),
    actionTransform = { actions ->
        merge(
            appMutator.globalUiMutator.navBarSizeMutations { copy(navBarSize = it) },
            authRepository.authMutations(),
            // Start monitoring the archive from the get go
            actions.onStart {
                if (route.archiveId != null) emit(
                    Action.Load.InitialLoad(
                        kind = route.kind,
                        id = route.archiveId,
                    )
                )
            }
                .toMutationStream(keySelector = Action::key) {
                    when (val action = type()) {
                        is Action.TextEdit -> action.flow.textEditMutations()
                        is Action.ChipEdit -> action.flow.chipEditMutations()
                        is Action.MessageConsumed -> action.flow.messageConsumptionMutations()
                        is Action.Load -> action.flow.loadMutations(
                            archiveRepository = archiveRepository
                        )
                    }
                }
        ).monitorWhenActive(appMutator)
    },
)

private fun AuthRepository.authMutations(): Flow<Mutation<State>> =
    isSignedIn.map {
        Mutation {
            copy(
                isSignedIn = it,
                hasFetchedAuthStatus = true
            )
        }
    }

private fun ArchiveRepository.textBodyMutations(
    kind: ArchiveKind,
    archiveId: ArchiveId
): Flow<Mutation<State>> = monitorArchive(
    kind = kind,
    id = archiveId
).map { archive ->
    Mutation {
        copy(
            upsert = upsert.copy(
                title = archive.title,
                description = archive.description,
                body = archive.body,
                categories = archive.categories,
                tags = archive.tags,
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
            val (updatedUpsert, updatedChipsState) = when (chipAction) {
                ChipAction.Added -> Pair(
                    upsert.copy(
                        categories = when (descriptor) {
                            is Descriptor.Category -> (upsert.categories + descriptor).distinct()
                            else -> upsert.categories
                        },
                        tags = when (descriptor) {
                            is Descriptor.Tag -> (upsert.tags + descriptor).distinct()
                            else -> upsert.tags
                        },
                    ),
                    chipsState.copy(
                        categoryText = when (descriptor) {
                            is Descriptor.Category -> Descriptor.Category(value = "")
                            else -> chipsState.categoryText
                        },
                        tagText = when (descriptor) {
                            is Descriptor.Tag -> Descriptor.Tag(value = "")
                            else -> chipsState.tagText
                        },
                    )
                )
                is ChipAction.Changed -> Pair(
                    upsert,
                    chipsState.copy(
                        categoryText = when (descriptor) {
                            is Descriptor.Category -> descriptor
                            else -> chipsState.categoryText
                        },
                        tagText = when (descriptor) {
                            is Descriptor.Tag -> descriptor
                            else -> chipsState.tagText
                        }
                    )
                )
                is ChipAction.Removed -> Pair(
                    upsert.copy(
                        categories = upsert.categories.filter { it != descriptor },
                        tags = upsert.tags.filter { it != descriptor },
                    ),
                    chipsState
                )
            }
            copy(
                upsert = updatedUpsert,
                chipsState = updatedChipsState
            )
        }
    }

private fun Flow<Action.MessageConsumed>.messageConsumptionMutations(): Flow<Mutation<State>> =
    map { (message) ->
        Mutation { copy(messages = messages - message) }
    }

/**
 * Load actions make sure the content on the screen is always up to date, especially for
 * archives that initially don't exist, and are then created.
 */
private fun Flow<Action.Load>.loadMutations(
    archiveRepository: ArchiveRepository
): Flow<Mutation<State>> =
    debounce(200)
        .flatMapLatest { monitor ->
            when (monitor) {
                is Action.Load.InitialLoad -> archiveRepository.textBodyMutations(
                    kind = monitor.kind,
                    archiveId = monitor.id
                )
                is Action.Load.Submit -> flow<Mutation<State>> {
                    val (kind, upsert) = monitor
                    emit(Mutation { copy(isSubmitting = true) })

                    val result = archiveRepository.upsert(kind = kind, upsert = upsert)

                    val message = when (result) {
                        is Result.Success -> if (upsert.id == null) "Created ${kind.singular}" else "Updated ${kind.singular}"
                        is Result.Error -> result.message ?: "unknown error"
                    }

                    emit(Mutation { copy(isSubmitting = false, messages = messages + message) })

                    // Start monitoring the created archive
                    val id = upsert.id ?: (result as? Result.Success)?.item
                    if (id != null) emitAll(
                        archiveRepository.textBodyMutations(
                            kind = kind,
                            archiveId = id
                        )
                    )
                }
            }
        }