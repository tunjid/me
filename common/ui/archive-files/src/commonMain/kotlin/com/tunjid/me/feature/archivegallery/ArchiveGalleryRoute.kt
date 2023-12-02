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

package com.tunjid.me.feature.archivegallery

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.round
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.ui.rememberAsyncRasterPainter
import com.tunjid.me.feature.rememberRetainedStateHolder
import com.tunjid.me.scaffold.adaptive.rememberSharedContent
import com.tunjid.me.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.me.scaffold.lifecycle.component1
import com.tunjid.me.scaffold.lifecycle.component2
import com.tunjid.me.scaffold.navigation.AppRoute
import com.tunjid.me.scaffold.navigation.SerializedRouteParams
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.mutation
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.pop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.Serializable


@Serializable
data class ArchiveGalleryRoute(
    override val routeParams: SerializedRouteParams,
) : AppRoute {

    val archiveId: ArchiveId = routeParams.pathArgs.getValue("id").let(::ArchiveId)
    val pageOffset get() = routeParams.queryParams["offset"]?.firstOrNull()?.toIntOrNull() ?: 0

    val urls get() = routeParams.queryParams["url"] ?: emptyList()

    @Composable
    override fun content() {
        ArchiveGalleryScreen(
            stateHolder = rememberRetainedStateHolder(
                route = this@ArchiveGalleryRoute
            ),
        )
    }
}

@Composable
internal fun ArchiveGalleryScreen(
    modifier: Modifier = Modifier,
    stateHolder: ArchiveGalleryStateHolder,
) {
    GlobalUi()

    val (state, actions) = stateHolder
    val items by rememberUpdatedState(state.items)

    val pagerState = rememberPagerState { items.size }

    val scope = rememberCoroutineScope()
    val offsetMutator = remember { scope.offsetMutator() }
    val offsetActions = offsetMutator.accept
    val offset by offsetMutator.state.collectAsState()

    HorizontalPager(
        state = pagerState,
        beyondBoundsPageCount = 0,
        modifier = modifier
            .fillMaxSize()
            .offset { offset.round() }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetActions(
                            Gesture.Drag(Offset(x = dragAmount.x, y = dragAmount.y))
                        )
                    },
                    onDragEnd = {
                        if (offset.getDistanceSquared() > 300 * 300) actions(Action.Navigate { navState.pop() })
                        else offsetActions(Gesture.Release)
                    },
                    onDragCancel = {
                        if (offset.getDistanceSquared() > 300 * 300) actions(Action.Navigate { navState.pop() })
                        else offsetActions(Gesture.Release)
                    }
                )
            },
        key = { index -> items[index].key }
    ) { index ->
        val file = items[index]
        val sharedElement = rememberSharedContent<String?>(
            thumbnailSharedElementKey(file.url)
        ) { imageUrl, modifier ->
            val imagePainter = rememberAsyncRasterPainter(
                imageUri = imageUrl,
            )
            if (imagePainter != null) Image(
                modifier = modifier,
                painter = imagePainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }

        sharedElement(
            file.url,
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )
    }

    pagerState.PivotedTilingEffect(
        items = items,
        onQueryChanged = {
            actions(Action.LoadAround(query = it ?: state.currentQuery))
        }
    )
}

sealed class Gesture {
    data class Drag(val offset: Offset) : Gesture()
    data object Release : Gesture()
}

private fun CoroutineScope.offsetMutator() = actionStateFlowProducer<Gesture, Offset>(
    initialState = Offset.Zero,
    actionTransform = { actions ->
        actions.flatMapLatest { action ->
            when (action) {
                is Gesture.Drag -> flowOf(
                    mutation {
                        copy(x = x + action.offset.x, y = y + action.offset.y)
                    }
                )

                Gesture.Release -> {
                    val (currentX, currentY) = state()
                    merge(
                        currentX.animate(to = 0f).mapToMutation { copy(x = it) },
                        currentY.animate(to = 0f).mapToMutation { copy(y = it) }
                    )
                }
            }
        }
    }
)

private fun Float.animate(to: Float) = callbackFlow {
    Animatable(initialValue = this@animate)
        .animateTo(to) {
            channel.trySend(value)
        }
    close()
}
