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

package com.tunjid.me.feature.archivefiles

import com.tunjid.me.core.model.ArchiveFile
import com.tunjid.me.core.model.ArchiveFileQuery
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.FILE_QUERY_LIMIT
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.core.utilities.LocalUri
import com.tunjid.me.core.utilities.Uri
import com.tunjid.me.data.network.models.TransferStatus
import com.tunjid.me.data.repository.ArchiveFileRepository
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.feature.FeatureWhileSubscribed
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.di.downcast
import com.tunjid.me.scaffold.di.restoreState
import com.tunjid.me.scaffold.isInPrimaryNavMutations
import com.tunjid.me.scaffold.nav.NavigationMutation
import com.tunjid.me.scaffold.nav.consumeNavActions
import com.tunjid.me.scaffold.permissions.Permission
import com.tunjid.me.scaffold.permissions.Permissions
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.mutator.mutation
import com.tunjid.tiler.Tile
import com.tunjid.tiler.toPivotedTileInputs
import com.tunjid.tiler.toTiledList
import com.tunjid.treenav.MultiStackNav
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject

typealias ArchiveFilesStateHolder = ActionStateProducer<Action, StateFlow<State>>

@Inject
class ArchiveFilesStateHolderCreator(
    creator: (scope: CoroutineScope, savedState: ByteArray?, route: ArchiveFilesRoute) -> ArchiveFilesStateHolder,
) : ScreenStateHolderCreator by creator.downcast()

/**
 * Manages [State] for [ArchiveFilesRoute]
 */
@Inject
class ActualArchiveFilesStateHolder(
    authRepository: AuthRepository,
    archiveRepository: ArchiveRepository,
    archiveFileRepository: ArchiveFileRepository,
    byteSerializer: ByteSerializer,
    permissionsFlow: StateFlow<Permissions>,
    navStateFlow: StateFlow<MultiStackNav>,
    navActions: (NavigationMutation) -> Unit,
    onPermissionRequested: (Permission) -> Unit,
    scope: CoroutineScope,
    savedState: ByteArray?,
    route: ArchiveFilesRoute,
) : ArchiveFilesStateHolder by scope.actionStateFlowProducer(
    initialState = byteSerializer.restoreState(savedState) ?: State(
        archiveId = route.archiveId,
        dndEnabled = route.dndEnabled,
        fileType = route.fileType,
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    mutationFlows = listOf(
        authRepository.authMutations(),
        navStateFlow.isInPrimaryNavMutations(
            route = route,
            mutation = { copy(isInPrimaryNav = it) }
        ),
        permissionsFlow.storagePermissionMutations(),
    ),
    actionTransform = { actions ->
        actions.toMutationStream(keySelector = Action::key) {
            when (val action = type()) {
                is Action.Drop -> action.flow.dropMutations(
                    archiveId = route.archiveId,
                    archiveRepository = archiveRepository,
                    archiveFileRepository = archiveFileRepository
                )

                is Action.Drag -> action.flow.dragStatusMutations()
                is Action.RequestPermission -> action.flow.permissionRequestMutations(
                    onPermissionRequested = onPermissionRequested
                )

                is Action.Fetch -> action.flow.loadMutations(
                    scope = scope,
                    archiveFileRepository = archiveFileRepository
                )

                is Action.Navigate -> action.flow.consumeNavActions(
                    mutationMapper = Action.Navigate::navMutation,
                    action = navActions
                )
            }
        }
    }
)

private fun AuthRepository.authMutations(): Flow<Mutation<State>> =
    isSignedIn
        .distinctUntilChanged()
        .mapToMutation {
            copy(
                isSignedIn = it,
                hasFetchedAuthStatus = true,
            )
        }

private fun Flow<Action.Fetch>.loadMutations(
    scope: CoroutineScope,
    archiveFileRepository: ArchiveFileRepository,
): Flow<Mutation<State>> {
    val columnSizes =
        filterIsInstance<Action.Fetch.ColumnSizeChanged>()
            .map { it.size }
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
                initialValue = 1
            )

    val pivotInputs: Flow<Tile.Input<ArchiveFileQuery, ArchiveFile>> =
        filterIsInstance<Action.Fetch.LoadAround>()
            .map { it.query }
            .toPivotedTileInputs(columnSizes.map(::pivotRequest))

    val limitInputs: Flow<Tile.Limiter<ArchiveFileQuery, ArchiveFile>> = columnSizes
        .map { columnSize ->
            Tile.Limiter(
                maxQueries = columnSize * 3,
                itemSizeHint = FILE_QUERY_LIMIT,
            )
        }

    return merge(
        pivotInputs,
        limitInputs
    ).toTiledList(
        archiveFileRepository.archiveFilesTiler(
            limiter = Tile.Limiter(
                maxQueries = 3,
                itemSizeHint = FILE_QUERY_LIMIT,
            )
        )
    )
        .map {
            mutation { copy(files = it) }
        }
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
 * Mutations for permission status for reading from storage
 */
private fun Flow<Permissions>.storagePermissionMutations(): Flow<Mutation<State>> =
    map { it.isGranted(Permission.ReadExternalStorage) }
        .distinctUntilChanged()
        .mapToMutation { copy(hasStoragePermissions = it) }

/**
 * Mutations from use drag events
 */
private fun Flow<Action.Drag>.dragStatusMutations(): Flow<Mutation<State>> =
    distinctUntilChanged()
        .mapToMutation { (dragLocation) ->
            copy(dragLocation = dragLocation)
        }

/**
 * Mutations from drop events
 */
private fun Flow<Action.Drop>.dropMutations(
    archiveId: ArchiveId,
    archiveRepository: ArchiveRepository,
    archiveFileRepository: ArchiveFileRepository,
): Flow<Mutation<State>> =
    channelFlow {
        val kind = archiveRepository.archiveStream(archiveId)
            .filterNotNull()
            .first()
            .kind

        var toUpload = listOf<Uri>()
        val uploaded = mutableSetOf<String>()

        map { it.uris.filterIsInstance<LocalUri>() }
            .collectLatest { uris ->
                toUpload = (toUpload + uris)
                    .distinctBy { it.path }
                    .filterNot { uploaded.contains(it.path) }

                uris.forEachIndexed { index, uri ->
                    channel.send {
                        copy(
                            uploadInfo = UploadInfo.Message("Uploading ${index + 1} of ${uris.size} files.")
                        )
                    }
                    archiveFileRepository.uploadArchiveFile(
                        kind = kind,
                        id = archiveId,
                        uri = uri,
                    ).collect { status ->
                        channel.send {
                            when (status) {
                                is TransferStatus.Done -> {
                                    uploaded.add(uri.path)
                                    copy(
                                        uploadInfo = UploadInfo.Message("Uploaded ${index + 1} of ${uris.size} files.")
                                    )
                                }

                                is TransferStatus.Error -> copy(
                                    uploadInfo = UploadInfo.Message("Failed to upload ${index + 1} of ${uris.size} files.")
                                )

                                is TransferStatus.Uploading -> copy(
                                    uploadInfo = UploadInfo.Progress(
                                        progress = status.progress,
                                        message = "Uploaded ${(status.progress * 100).toInt()}% of file ${index + 1} of ${uris.size}."
                                    )
                                )
                            }
                        }
                    }
                }

                if (uris.isNotEmpty()) channel.send {
                    val message = if (uris.size == 1) "Uploaded 1 image"
                    else "Uploaded ${uris.size} images"
                    copy(uploadInfo = UploadInfo.Message(message))
                }

                delay(2000)
                channel.send {
                    copy(uploadInfo = UploadInfo.None)
                }
            }
    }
