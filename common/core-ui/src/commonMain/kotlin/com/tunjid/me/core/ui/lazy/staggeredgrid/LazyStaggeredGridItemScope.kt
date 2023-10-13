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

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntOffset
import com.tunjid.me.core.ui.lazy.layout.LazyLayoutAnimationSpecsNode

/**
 * Receiver scope for itemContent in [LazyStaggeredGridScope.item]
 */
@Stable
@LazyStaggeredGridScopeMarker
sealed interface LazyStaggeredGridItemScope {
    /**
     * This modifier animates the item placement within the grid.
     *
     * When you scroll backward staggered grids could move already visible items in order
     * to correct the accumulated errors in previous item size estimations. This modifier
     * can animate such moves.
     *
     * Aside from that when you provide a key via [LazyStaggeredGridScope.item] /
     * [LazyStaggeredGridScope.items] this modifier will enable item reordering animations.
     *
     * @param animationSpec a finite animation that will be used to animate the item placement.
     */
    @ExperimentalFoundationApi
    fun Modifier.animateItemPlacement(
        animationSpec: FiniteAnimationSpec<IntOffset> = spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold
        )
    ): Modifier
}

@OptIn(ExperimentalFoundationApi::class)
internal object LazyStaggeredGridItemScopeImpl : LazyStaggeredGridItemScope {
    @ExperimentalFoundationApi
    override fun Modifier.animateItemPlacement(animationSpec: FiniteAnimationSpec<IntOffset>) =
        this then AnimateItemElement(animationSpec)
}

private data class AnimateItemElement(
    val placementSpec: FiniteAnimationSpec<IntOffset>
) : ModifierNodeElement<LazyLayoutAnimationSpecsNode>() {

    override fun create(): LazyLayoutAnimationSpecsNode =
        LazyLayoutAnimationSpecsNode(null, placementSpec)

    override fun update(node: LazyLayoutAnimationSpecsNode) {
        node.placementSpec = placementSpec
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "animateItemPlacement"
        value = placementSpec
    }
}
