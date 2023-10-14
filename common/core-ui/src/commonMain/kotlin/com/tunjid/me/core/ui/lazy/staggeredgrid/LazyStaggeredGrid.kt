/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.me.core.ui.lazy.staggeredgrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clipScrollableContainer
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.overscroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.me.core.ui.lazy.layout.lazyLayoutBeyondBoundsModifier
import com.tunjid.me.core.ui.lazy.layout.lazyLayoutSemantics

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LazyStaggeredGrid(
    /** State controlling the scroll position */
    state: LazyStaggeredGridState,
    /** The layout orientation of the grid */
    orientation: Orientation,
    /** Cross axis positions and sizes of slots per line, e.g. the columns for vertical grid. */
    slots: LazyGridStaggeredGridSlotsProvider,
    /** Modifier to be applied for the inner layout */
    modifier: Modifier = Modifier,
    /** The inner padding to be added for the whole content (not for each individual item) */
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean = false,
    /** fling behavior to be used for flinging */
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    /** Whether scrolling via the user gestures is allowed. */
    userScrollEnabled: Boolean = true,
    /** The vertical spacing for items/lines. */
    mainAxisSpacing: Dp = 0.dp,
    /** The horizontal spacing for items/lines. */
    crossAxisSpacing: Dp = 0.dp,
    /** The content of the grid */
    content: LazyStaggeredGridScope.() -> Unit
) {
    val overscrollEffect = ScrollableDefaults.overscrollEffect()

    val itemProviderState = rememberStaggeredGridItemProvider(state, content)
    val itemProviderLambda by remember {
        derivedStateOf { itemProviderState::value }
    }

    val coroutineScope = rememberCoroutineScope()
    val measurePolicy = rememberStaggeredGridMeasurePolicy(
        state,
        itemProviderLambda,
        contentPadding,
        reverseLayout,
        orientation,
        mainAxisSpacing,
        crossAxisSpacing,
        coroutineScope,
        slots,
    )
    val semanticState = rememberLazyStaggeredGridSemanticState(state, reverseLayout)

    ScrollPositionUpdater(itemProviderLambda, state)

    LazyLayout(
        modifier = modifier
            .then(state.remeasurementModifier)
            .then(state.awaitLayoutModifier)
            .lazyLayoutSemantics(
                itemProviderLambda = itemProviderLambda,
                state = semanticState,
                orientation = orientation,
                userScrollEnabled = userScrollEnabled,
                reverseScrolling = reverseLayout
            )
            .clipScrollableContainer(orientation)
            .lazyLayoutBeyondBoundsModifier(
                state = rememberLazyStaggeredGridBeyondBoundsState(state = state),
                beyondBoundsInfo = state.beyondBoundsInfo,
                reverseLayout = reverseLayout,
                layoutDirection = LocalLayoutDirection.current,
                orientation = orientation,
                enabled = userScrollEnabled
            )
            .overscroll(overscrollEffect)
            .scrollable(
                orientation = orientation,
                reverseDirection = ScrollableDefaults.reverseDirection(
                    LocalLayoutDirection.current,
                    orientation,
                    reverseLayout
                ),
                interactionSource = state.mutableInteractionSource,
                flingBehavior = flingBehavior,
                state = state,
                overscrollEffect = overscrollEffect,
                enabled = userScrollEnabled
            ),
        prefetchState = state.prefetchState,
        itemProvider =  itemProviderState.value,
        measurePolicy = measurePolicy
    )
}

/** Extracted to minimize the recomposition scope */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScrollPositionUpdater(
    itemProviderLambda: () -> LazyLayoutItemProvider,
    state: LazyStaggeredGridState
) {
    val itemProvider = itemProviderLambda()
    if (itemProvider.itemCount > 0) {
        state.updateScrollPositionIfTheFirstItemWasMoved(itemProvider)
    }
}

/** Slot configuration of staggered grid */
internal class LazyStaggeredGridSlots(
    val positions: IntArray,
    val sizes: IntArray
)

