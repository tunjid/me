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


import com.tunjid.me.core.model.*
import com.tunjid.me.core.ui.ChipAction
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.core.utilities.Uri
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.di.downcast
import com.tunjid.me.scaffold.di.restoreState
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.navBarSize
import com.tunjid.me.scaffold.globalui.navBarSizeMutations
import com.tunjid.me.scaffold.permissions.Permission
import com.tunjid.me.scaffold.permissions.Permissions
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.mutator.mutation
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
import me.tatarka.inject.annotations.Inject

typealias ArchiveEditStateHolder = ActionStateProducer<Action, StateFlow<State>>

@Inject
class ArchiveEditStateHolderCreator(
    creator: (scope: CoroutineScope, savedStae: ByteArray?, route: ArchiveEditRoute) -> ArchiveEditStateHolder
) : ScreenStateHolderCreator by creator.downcast()

@Inject
class ActualArchiveEditStateHolder(
    archiveRepository: ArchiveRepository,
    authRepository: AuthRepository,
    byteSerializer: ByteSerializer,
    uiStateFlow: StateFlow<UiState>,
    permissionsFlow: StateFlow<Permissions>,
    onPermissionRequested: (Permission) -> Unit,
    scope: CoroutineScope,
    savedState: ByteArray?,
    route: ArchiveEditRoute,
) : ArchiveEditStateHolder by scope.actionStateFlowProducer(
    initialState = byteSerializer.restoreState(savedState) ?: State(
        kind = route.kind,
        upsert = ArchiveUpsert(id = route.archiveId),
        navBarSize = uiStateFlow.value.navBarSize,
        hasStoragePermissions = permissionsFlow.value.isGranted(Permission.ReadExternalStorage)
    ),
    started = SharingStarted.WhileSubscribed(),
    mutationFlows = listOf(
        uiStateFlow.navBarSizeMutations { copy(navBarSize = it) },
        permissionsFlow.storagePermissionMutations(),
        authRepository.authMutations(),
    ),
    actionTransform = { actions ->
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
                    is Action.RequestPermission -> action.flow.permissionRequestMutations(
                        onPermissionRequested = onPermissionRequested
                    )

                    is Action.Load -> action.flow.loadMutations(
                        archiveRepository = archiveRepository,
                    )
                }
            }
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
 * Mutations for permission status for reading from storage
 */
private fun Flow<Permissions>.storagePermissionMutations(): Flow<Mutation<State>> =
    map { it.isGranted(Permission.ReadExternalStorage) }
        .distinctUntilChanged()
        .map { mutation { copy(hasStoragePermissions = it) } }

/**
 * Mutations that have to do with the user's signed in status
 */
private fun AuthRepository.authMutations(): Flow<Mutation<State>> =
    isSignedIn.map {
        mutation {
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
    map { mutation { copy(isEditing = !isEditing) } }

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
            mutation {
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
        mutation {
            if (uri != null) copy(toUpload = uri, thumbnail = uri.path)
            else copy(toUpload = null, messages = messages + "Only png and jpg uploads are supported")
        }
    }

/**
 * Mutations from editing the chips
 */
private fun Flow<Action.ChipEdit>.chipEditMutations(): Flow<Mutation<State>> =
    map { (chipAction, descriptor) ->
        mutation {
            if (descriptor.value.isBlank()) return@mutation this
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
        mutation { copy(messages = messages - message) }
    }

/**
 * Empty mutations proxied to [onPermissionRequested] to request permissions
 */
private fun Flow<Action.RequestPermission>.permissionRequestMutations(
    onPermissionRequested: (Permission) -> Unit
): Flow<Mutation<State>> =
    flatMapLatest { (permission) ->
        onPermissionRequested(permission)
        emptyFlow()
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

                is Action.Load.Submit -> flow {
                    val (kind, upsert, headerPhoto) = monitor
                    emit(mutation { copy(isSubmitting = true) })

                    val result = archiveRepository.upsert(kind = kind, upsert = upsert)

                    val message = when (result) {
                        is Result.Success -> when (upsert.id) {
                            null -> "Created ${kind.singular}"
                            else -> "Updated ${kind.singular}"
                        }

                        is Result.Error -> result.message ?: "unknown error"
                    }

                    emit(mutation { copy(isSubmitting = false, messages = messages + message) })

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
        mutation {
            copy(
                thumbnail = archive.thumbnail,
                upsert = upsert.copy(
                    id = archive.id,
                    title = archive.title,
                    description = archive.description,
                    videoUrl = archive.videoUrl,
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
                is Result.Error -> emit(mutation {
                    copy(messages = messages + "Error uploading header: ${result.message ?: "Unknown error"}")
                })
            }
        }
    }