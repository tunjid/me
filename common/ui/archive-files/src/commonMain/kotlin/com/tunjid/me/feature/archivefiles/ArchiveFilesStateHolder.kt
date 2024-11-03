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

// See YouTrack: KTIJ-18375
@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package com.tunjid.me.feature.archivefiles

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
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
import com.tunjid.me.feature.archivefiles.di.archiveId
import com.tunjid.me.feature.archivefiles.di.dndEnabled
import com.tunjid.me.feature.archivefiles.di.fileType
import com.tunjid.me.feature.archivefiles.di.urls
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.isInPrimaryNavMutations
import com.tunjid.me.scaffold.navigation.NavigationMutation
import com.tunjid.me.scaffold.navigation.consumeNavigationActions
import com.tunjid.me.scaffold.permissions.Permission
import com.tunjid.me.scaffold.permissions.Permissions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.mutator.mutationOf
import com.tunjid.tiler.Tile
import com.tunjid.tiler.buildTiledList
import com.tunjid.tiler.map
import com.tunjid.tiler.toPivotedTileInputs
import com.tunjid.tiler.toTiledList
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias ArchiveFilesStateHolder = ActionStateMutator<Action, StateFlow<State>>

@Stable
@Inject
class ArchiveFilesStateHolderCreator(
    private val creator: (scope: CoroutineScope, route: Route) -> ActualArchiveFilesStateHolder
) : ScreenStateHolderCreator {
    override fun invoke(
        scope: CoroutineScope,
        route: Route
    ): ActualArchiveFilesStateHolder = creator.invoke(scope, route)
}

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
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) :  ViewModel(viewModelScope = scope), ArchiveFilesStateHolder by scope.actionStateFlowMutator(
    initialState = State(
        archiveId = route.routeParams.archiveId,
        dndEnabled = route.routeParams.dndEnabled,
        currentQuery = ArchiveFileQuery(
            archiveId = route.routeParams.archiveId,
        ),
        fileType = route.routeParams.fileType,
        items = buildTiledList {
            addAll(
                query = ArchiveFileQuery(route.routeParams.archiveId),
                items = route.routeParams.urls.map(FileItem::PlaceHolder)
            )
        }
    ).also { println("ROUTE: $route") },
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    inputs = listOf(
        authRepository.authMutations(),
        navStateFlow.isInPrimaryNavMutations(
            route = route,
            mutation = { copy(isInPrimaryNav = it) }
        ),
        permissionsFlow.storagePermissionMutations(),
    ),
    actionTransform = stateHolder@{ actions ->
        actions.toMutationStream(keySelector = Action::key) {
            when (val action = type()) {
                is Action.Drop -> action.flow.dropMutations(
                    archiveId = route.routeParams.archiveId,
                    archiveRepository = archiveRepository,
                    archiveFileRepository = archiveFileRepository
                )

                is Action.Drag -> action.flow.dragStatusMutations()
                is Action.RequestPermission -> action.flow.permissionRequestMutations(
                    onPermissionRequested = onPermissionRequested
                )

                is Action.Fetch -> action.flow.loadMutations(
                    stateHolder = this@stateHolder,
                    archiveFileRepository = archiveFileRepository
                )

                is Action.Navigate -> action.flow.consumeNavigationActions(
                    navigationMutationConsumer = navActions
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

private suspend fun Flow<Action.Fetch>.loadMutations(
    stateHolder: SuspendingStateHolder<State>,
    archiveFileRepository: ArchiveFileRepository,
): Flow<Mutation<State>> = scan(
    initial = Pair(
        MutableStateFlow(value = stateHolder.state().currentQuery),
        MutableStateFlow(value = 1)
    )
) { accumulator, action ->
    val (queries, numColumns) = accumulator
    when (action) {
        is Action.Fetch.LoadAround -> queries.value = action.query
        is Action.Fetch.ColumnSizeChanged -> numColumns.value = action.size
    }
    // Emit the same item with each action
    accumulator
}
    // Only emit once
    .distinctUntilChanged()
    .flatMapLatest { (queries, numColumns) ->
        val pivotInputs = queries.toPivotedTileInputs(
            numColumns.map(::pivotRequest)
        )
        val limitInputs: Flow<Tile.Limiter<ArchiveFileQuery, ArchiveFile>> = numColumns
            .map { columnSize ->
                Tile.Limiter(
                    maxQueries = columnSize * 3,
                    itemSizeHint = FILE_QUERY_LIMIT,
                )
            }

        val listMutations: Flow<Mutation<State>> = merge(
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
            .onEach {
                println("Fetched ${it.size}")
            }
            .map {
                mutationOf { copy(items = it.map(FileItem::File)) }
            }

        val currentQueryMutations: Flow<Mutation<State>> =
            queries.mapToMutation {
                copy(currentQuery = it)
            }

        merge(
            currentQueryMutations,
            listMutations
        )
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
