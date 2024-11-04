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

package com.tunjid.me.feature.archivegallery

import androidx.lifecycle.ViewModel
import com.tunjid.me.core.model.ArchiveFileQuery
import com.tunjid.me.core.model.FILE_QUERY_LIMIT
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.data.repository.ArchiveFileRepository
import com.tunjid.me.feature.FeatureWhileSubscribed
import com.tunjid.me.feature.archivefiles.archiveFilesTiler
import com.tunjid.me.feature.archivefiles.pivotRequest
import com.tunjid.me.feature.archivegallery.GalleryItem.PlaceHolder
import com.tunjid.me.feature.archivegallery.di.archiveId
import com.tunjid.me.feature.archivegallery.di.pageOffset
import com.tunjid.me.feature.archivegallery.di.urls
import com.tunjid.me.scaffold.di.ScreenStateHolderCreator
import com.tunjid.me.scaffold.navigation.NavigationMutation
import com.tunjid.me.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.Tile
import com.tunjid.tiler.buildTiledList
import com.tunjid.tiler.map
import com.tunjid.tiler.toPivotedTileInputs
import com.tunjid.tiler.toTiledList
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias ArchiveGalleryStateHolder = ActionStateMutator<Action, StateFlow<State>>

@Inject
class ArchiveGalleryStateHolderCreator(
    private val creator: (scope: CoroutineScope, route: Route) -> ActualArchiveGalleryStateHolder
) : ScreenStateHolderCreator {
    override fun invoke(
        scope: CoroutineScope,
        route: Route
    ): ActualArchiveGalleryStateHolder = creator.invoke(scope, route)
}

/**
 * Manages [State] for [ArchiveGalleryRoute]
 */
@Inject
class ActualArchiveGalleryStateHolder(
    archiveFileRepository: ArchiveFileRepository,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope), ArchiveGalleryStateHolder by scope.actionStateFlowMutator(
    initialState = State(
        currentQuery = ArchiveFileQuery(
            archiveId = route.routeParams.archiveId,
            offset = route.routeParams.pageOffset,
        ),
        items = buildTiledList {
            addAll(
                query = ArchiveFileQuery(route.routeParams.archiveId),
                items = route.routeParams.urls.map(::PlaceHolder)
            )
        }
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    actionTransform = actionTransform@{ actions ->
        actions.toMutationStream {
            when (val action = type()) {
                is Action.LoadAround -> action.flow.loadMutations(archiveFileRepository)
                is Action.Navigate -> action.flow.consumeNavigationActions(
                    navigationMutationConsumer = navActions
                )
            }
        }
    }
)

private fun Flow<Action.LoadAround>.loadMutations(
    archiveFileRepository: ArchiveFileRepository,
): Flow<Mutation<State>> =
    map { it.query }
        .toPivotedTileInputs(pivotRequest(gridSize = 1))
        .toTiledList(
            archiveFileRepository.archiveFilesTiler(
                limiter = Tile.Limiter(
                    maxQueries = 3,
                    itemSizeHint = FILE_QUERY_LIMIT,
                )
            )
        )
        .mapToMutation {
            copy(items = it.map(GalleryItem::File))
        }
