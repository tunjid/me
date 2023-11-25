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


import androidx.compose.ui.text.input.TextFieldValue
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveUpsert
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.model.Result
import com.tunjid.me.core.model.minus
import com.tunjid.me.core.model.plus
import com.tunjid.me.core.model.singular
import com.tunjid.me.core.ui.ChipAction
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.core.utilities.LocalUri
import com.tunjid.me.core.utilities.Uri
import com.tunjid.me.core.utilities.UriConverter
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.di.downcast
import com.tunjid.me.scaffold.di.restoreState
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.globalui.navBarSize
import com.tunjid.me.scaffold.globalui.navBarSizeMutations
import com.tunjid.me.scaffold.nav.NavigationMutation
import com.tunjid.me.scaffold.nav.consumeNavigationActions
import com.tunjid.me.scaffold.permissions.Permission
import com.tunjid.me.scaffold.permissions.Permissions
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
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
    creator: (scope: CoroutineScope, savedStae: ByteArray?, route: ArchiveEditRoute) -> ArchiveEditStateHolder,
) : ScreenStateHolderCreator by creator.downcast()

@Inject
class ActualArchiveEditStateHolder(
    archiveRepository: ArchiveRepository,
    authRepository: AuthRepository,
    byteSerializer: ByteSerializer,
    uriConverter: UriConverter,
    uiStateFlow: StateFlow<UiState>,
    permissionsFlow: StateFlow<Permissions>,
    onPermissionRequested: (Permission) -> Unit,
    navActions: (NavigationMutation) -> Unit,
    scope: CoroutineScope,
    savedState: ByteArray?,
    route: ArchiveEditRoute,
) : ArchiveEditStateHolder by scope.actionStateFlowProducer(
    initialState = byteSerializer.restoreState(savedState) ?: State(
        kind = route.kind,
        sharedElementKey = thumbnailSharedElementKey(route.archiveThumbnail),
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
                    is Action.Drop -> action.flow.dropMutations(
                        uriConverter = uriConverter
                    )

                    is Action.Drag -> action.flow.dragStatusMutations()
                    is Action.TextEdit -> action.flow.textEditMutations()
                    is Action.ChipEdit -> action.flow.chipEditMutations()
                    is Action.ToggleEditView -> action.flow.viewToggleMutations()
                    is Action.MessageConsumed -> action.flow.messageConsumptionMutations()
                    is Action.RequestPermission -> action.flow.permissionRequestMutations(
                        onPermissionRequested = onPermissionRequested
                    )

                    is Action.Navigate -> action.flow.consumeNavigationActions(
                        navigationMutationConsumer = navActions
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
    route: ArchiveEditRoute,
) = onStart {
    val archiveId = route.archiveId
    if (archiveId != null) emit(
        Action.Load.InitialLoad(
            kind = route.kind,
            id = archiveId,
        )
    )
}

/**
 * Mutations for permission status for reading from storage
 */
private fun Flow<Permissions>.storagePermissionMutations(): Flow<Mutation<State>> =
    map { it.isGranted(Permission.ReadExternalStorage) }
        .distinctUntilChanged()
        .mapToMutation { copy(hasStoragePermissions = it) }

/**
 * Mutations that have to do with the user's signed in status
 */
private fun AuthRepository.authMutations(): Flow<Mutation<State>> =
    isSignedIn
        .mapToMutation {
            copy(
                isSignedIn = it,
                hasFetchedAuthStatus = true
            )
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
    mapToMutation { copy(isEditing = !isEditing) }

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
        .mapToMutation { (inWindow, inThumbnail) ->
            copy(
                dragLocation = when {
                    inThumbnail -> DragLocation.InThumbnail
                    inWindow -> DragLocation.InWindow
                    else -> DragLocation.None
                }
            )
        }

/**
 * Mutations from drop events
 */
private val validMimeTypes = listOf("jpeg", "jpg", "png", "gif")
private fun Flow<Action.Drop>.dropMutations(
    uriConverter: UriConverter,
): Flow<Mutation<State>> =
    map { (uris) ->
        val uri = uris.firstOrNull { uri ->
            when (uri) {
                is LocalUri -> {
                    val mimeType = uriConverter.mimeType(uri)
                    mimeType.contains("image") && validMimeTypes.any(mimeType::contains)
                }

                else -> validMimeTypes.any { it.contains(uri.path.split(".").lastOrNull() ?: "") }
            }
        }
        mutation {
            when (uri) {
                is LocalUri -> copy(toUpload = uri, thumbnail = uri.path)
                is Uri -> copy(
                    toUpload = null,
                    thumbnail = uri.path,
                    upsert = upsert.copy(thumbnail = uri.path)
                )

                else -> copy(
                    toUpload = null,
                    messages = messages + "Only png, jpg and gif uploads are supported"
                )
            }
        }
    }

/**
 * Mutations from editing the chips
 */
private fun Flow<Action.ChipEdit>.chipEditMutations(): Flow<Mutation<State>> =
    mapToMutation { (chipAction, descriptor) ->
        if (descriptor.value.isBlank()) return@mapToMutation this
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

/**
 * Mutations from consuming messages from the message queue
 */
private fun Flow<Action.MessageConsumed>.messageConsumptionMutations(): Flow<Mutation<State>> =
    mapToMutation { (message) ->
        copy(messages = messages - message)
    }

/**
 * Empty mutations proxied to [onPermissionRequested] to request permissions
 */
private fun Flow<Action.RequestPermission>.permissionRequestMutations(
    onPermissionRequested: (Permission) -> Unit,
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
        .mapLatestToManyMutations { monitor ->
            when (monitor) {
                is Action.Load.InitialLoad -> emitAll(
                    archiveRepository.textBodyMutations(
                        archiveId = monitor.id
                    )
                )

                is Action.Load.Submit -> {
                    val (kind, upsert, headerPhoto) = monitor
                    emit { copy(isSubmitting = true) }

                    val result = archiveRepository.upsert(kind = kind, upsert = upsert)

                    val message = when (result) {
                        is Result.Success -> when (upsert.id) {
                            null -> "Created ${kind.singular}"
                            else -> "Updated ${kind.singular}"
                        }

                        is Result.Error -> result.message ?: "unknown error"
                    }

                    emit { copy(isSubmitting = false, messages = messages + message) }

                    // Start monitoring the created archive
                    val id = upsert.id ?: (result as? Result.Success)?.item
                    ?: return@mapLatestToManyMutations

                    emitAll(
                        listOfNotNull(
                            headerPhoto?.let {
                                archiveRepository.headerUploadMutations(
                                    headerPhoto = it,
                                    id = id,
                                    kind = kind
                                )
                            },
                            archiveRepository.textBodyMutations(
                                archiveId = id
                            )
                        ).merge()
                    )
                }
            }
        }

/**
 * Mutations from monitoring the archive
 */
private fun ArchiveRepository.textBodyMutations(
    archiveId: ArchiveId,
): Flow<Mutation<State>> = archiveStream(
    id = archiveId
)
    .filterNotNull()
    .mapToMutation { archive ->
        copy(
            sharedElementKey = thumbnailSharedElementKey(archive.thumbnail),
            thumbnail = archive.thumbnail,
            body = TextFieldValue(
                text = archive.body
            ),
            upsert = upsert.copy(
                id = archive.id,
                title = archive.title,
                description = archive.description,
                videoUrl = archive.videoUrl,
                thumbnail = archive.thumbnail,
                body = archive.body,
                categories = archive.categories,
                tags = archive.tags,
            )
        )
    }

/**
 * Mutations from header image uploads
 */
private fun ArchiveRepository.headerUploadMutations(
    headerPhoto: LocalUri,
    id: ArchiveId,
    kind: ArchiveKind,
): Flow<Mutation<State>> =
    flow {
        when (val result = uploadArchiveHeaderPhoto(
            kind = kind,
            id = id,
            uri = headerPhoto
        )) {
            // Do nothing on success
            is Result.Success -> Unit
            is Result.Error -> emit {
                copy(messages = messages + "Error uploading header: ${result.message ?: "Unknown error"}")
            }
        }
    }