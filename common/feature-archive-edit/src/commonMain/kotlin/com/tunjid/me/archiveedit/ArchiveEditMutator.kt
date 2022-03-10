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

package com.tunjid.me.archiveedit


import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveUpsert
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.model.Result
import com.tunjid.me.core.model.minus
import com.tunjid.me.core.model.plus
import com.tunjid.me.core.model.singular
import com.tunjid.me.core.ui.ChipAction
import com.tunjid.me.core.utilities.Uri
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.navBarSize
import com.tunjid.me.scaffold.globalui.navBarSizeMutations
import com.tunjid.me.scaffold.lifecycle.Lifecycle
import com.tunjid.me.scaffold.lifecycle.monitorWhenActive
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import com.tunjid.mutator.coroutines.toMutationStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan

typealias ArchiveEditMutator = Mutator<Action, StateFlow<State>>

fun archiveEditMutator(
    scope: CoroutineScope,
    route: ArchiveEditRoute,
    initialState: State? = null,
    archiveRepository: ArchiveRepository,
    authRepository: AuthRepository,
    uiStateFlow: StateFlow<UiState>,
    lifecycleStateFlow: StateFlow<Lifecycle>,
): ArchiveEditMutator = stateFlowMutator(
    scope = scope,
    initialState = initialState ?: State(
        kind = route.kind,
        upsert = ArchiveUpsert(id = route.archiveId),
        navBarSize = uiStateFlow.value.navBarSize,
    ),
    started = SharingStarted.WhileSubscribed(),
    actionTransform = { actions ->
        merge(
            uiStateFlow.navBarSizeMutations { copy(navBarSize = it) },
            authRepository.authMutations(),
            actions
                .withInitialLoad(route)
                .toMutationStream(keySelector = Action::key) {
                    when (val action = type()) {
                        is Action.Drop -> action.flow.dropMutations()
                        is Action.Drag -> action.flow.dragStatusMutations()
                        is Action.TextEdit -> action.flow.textEditMutations()
                        is Action.ChipEdit -> action.flow.chipEditMutations()
                        is Action.ToggleEditView -> action.flow.viewToggleMutations()
                        is Action.MessageConsumed -> action.flow.messageConsumptionMutations()
                        is Action.Load -> action.flow.loadMutations(
                            archiveRepository = archiveRepository,
                        )
                    }
                }
        ).monitorWhenActive(lifecycleStateFlow)
    },
)

/**
 * Start the load by monitoring the archive if it exists already
 */
private fun Flow<Action>.withInitialLoad(
    route: ArchiveEditRoute
) = onStart {
    if (route.archiveId != null) emit(
        Action.Load.InitialLoad(
            kind = route.kind,
            id = route.archiveId,
        )
    )
}

/**
 * Mutations that have to do with the user's signed in status
 */
private fun AuthRepository.authMutations(): Flow<Mutation<State>> =
    isSignedIn.map {
        Mutation {
            copy(
                isSignedIn = it,
                hasFetchedAuthStatus = true
            )
        }
    }

/**
 * Mutations from use text inputs
 */
private fun Flow<Action.TextEdit>.textEditMutations(): Flow<Mutation<State>> =
    map { it.mutation }

/**
 * Mutations from use text inputs
 */
private fun Flow<Action.ToggleEditView>.viewToggleMutations(): Flow<Mutation<State>> =
    map { Mutation { copy(isEditing = !isEditing) } }

/**
 * Mutations from use drag events
 */
private fun Flow<Action.Drag>.dragStatusMutations(): Flow<Mutation<State>> =
    distinctUntilChanged()
        .scan(false to false) { inWindowToInThumbnail, action ->
            when (action) {
                is Action.Drag.Window -> inWindowToInThumbnail.copy(first = action.inside)
                is Action.Drag.Thumbnail -> inWindowToInThumbnail.copy(second = action.inside)
            }
        }
        .map { (inWindow, inThumbnail) ->
            Mutation {
                copy(
                    dragStatus = when {
                        inThumbnail -> DragStatus.InThumbnail
                        inWindow -> DragStatus.InWindow
                        else -> DragStatus.None
                    }
                )
            }
        }

/**
 * Mutations from drop events
 */
private val validMimeTypes = listOf("jpeg", "jpg", "png")
private fun Flow<Action.Drop>.dropMutations(): Flow<Mutation<State>> =
    map { (uris) ->
        val uri = uris.filter {
            val mimeType = it.mimeType ?: return@filter false
            mimeType.contains("image") && validMimeTypes.any(mimeType::contains)
        }.firstOrNull()
        Mutation {
            if (uri != null) copy(toUpload = uri, thumbnail = uri.path)
            else copy(toUpload = null, messages = messages + "Only png and jpg uploads are supported")
        }
    }

/**
 * Mutations from editing the chips
 */
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

/**
 * Mutations from consuming messages from the message queue
 */
private fun Flow<Action.MessageConsumed>.messageConsumptionMutations(): Flow<Mutation<State>> =
    map { (message) ->
        Mutation { copy(messages = messages - message) }
    }

/**
 * Load actions make sure the content on the screen is always up to date, especially for
 * archives that initially don't exist, and are then created.
 */
private fun Flow<Action.Load>.loadMutations(
    archiveRepository: ArchiveRepository,
): Flow<Mutation<State>> =
    debounce(200)
        .flatMapLatest { monitor ->
            when (monitor) {
                is Action.Load.InitialLoad -> archiveRepository.textBodyMutations(
                    kind = monitor.kind,
                    archiveId = monitor.id
                )
                is Action.Load.Submit -> flow<Mutation<State>> {
                    val (kind, upsert, headerPhoto) = monitor
                    emit(Mutation { copy(isSubmitting = true) })

                    val result = archiveRepository.upsert(kind = kind, upsert = upsert)

                    val message = when (result) {
                        is Result.Success -> when (upsert.id) {
                            null -> "Created ${kind.singular}"
                            else -> "Updated ${kind.singular}"
                        }
                        is Result.Error -> result.message ?: "unknown error"
                    }

                    emit(Mutation { copy(isSubmitting = false, messages = messages + message) })

                    // Start monitoring the created archive
                    val id = upsert.id ?: (result as? Result.Success)?.item ?: return@flow

                    emitAll(
                        merge(
                            archiveRepository.headerUploadMutations(
                                headerPhoto = headerPhoto,
                                id = id,
                                kind = kind
                            ),
                            archiveRepository.textBodyMutations(
                                kind = kind,
                                archiveId = id
                            )
                        )
                    )
                }
            }
        }

/**
 * Mutations from monitoring the archive
 */
private fun ArchiveRepository.textBodyMutations(
    kind: ArchiveKind,
    archiveId: ArchiveId
): Flow<Mutation<State>> = monitorArchive(
    kind = kind,
    id = archiveId
)
    .filterNotNull()
    .map { archive ->
        Mutation {
            copy(
                thumbnail = archive.thumbnail,
                upsert = upsert.copy(
                    id = archive.id,
                    title = archive.title,
                    description = archive.description,
                    body = archive.body,
                    categories = archive.categories,
                    tags = archive.tags,
                )
            )
        }
    }

/**
 * Mutations from header image uploads
 */
private fun ArchiveRepository.headerUploadMutations(
    headerPhoto: Uri?,
    id: ArchiveId,
    kind: ArchiveKind
): Flow<Mutation<State>> =
    when (headerPhoto) {
        null -> emptyFlow()
        else -> flow {
            when (val result = uploadArchiveHeaderPhoto(
                kind = kind,
                id = id,
                uri = headerPhoto
            )) {
                // Do nothing on success
                is Result.Success -> Unit
                is Result.Error -> emit(Mutation {
                    copy(messages = messages + "Error uploading header: ${result.message ?: "Unknown error"}")
                })
            }
        }
    }