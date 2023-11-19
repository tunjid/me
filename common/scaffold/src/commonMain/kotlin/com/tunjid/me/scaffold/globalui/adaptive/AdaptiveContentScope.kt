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

package com.tunjid.me.scaffold.globalui.adaptive

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * An implementation of [Adaptive.ContainerScope] that supports animations and shared elements
 */
@Stable
internal class AnimatedAdaptiveContentScope(
    containerState: Adaptive.ContainerState,
    val adaptiveContentHost: AdaptiveContentHost,
    val animatedContentScope: AnimatedContentScope
) : Adaptive.ContainerScope, AnimatedVisibilityScope by animatedContentScope {

    override var containerState by mutableStateOf(containerState)
    override var canAnimateSharedElements: Boolean by mutableStateOf(
        value = containerState.adaptation != Adaptive.Adaptation.PrimaryToTransient
    )

    override fun isCurrentlyShared(key: Any): Boolean =
        adaptiveContentHost.isCurrentlyShared(key)

    @Composable
    override fun rememberSharedContent(
        key: Any,
        sharedElement: @Composable (Modifier) -> Unit
    ): @Composable (Modifier) -> Unit {
        val currentNavigationState = adaptiveContentHost.adaptedState
        // This container state may be animating out. Look up the actual current route
        val currentRouteInContainer = containerState.container?.let(
            currentNavigationState::routeFor
        )
        val isCurrentlyAnimatingIn = currentRouteInContainer?.id == containerState.currentRoute?.id

        // Do not use the shared element if this content is being animated out
        if (!isCurrentlyAnimatingIn) return sharedElement

        return adaptiveContentHost.createOrUpdateSharedElement(key, sharedElement)
    }
}

/**
 * Creates a shared element between composables
 * @param key the key for the shared element
 * @param sharedElement the element to be shared
 */
@Composable
fun rememberSharedContent(
    key: Any,
    sharedElement: @Composable (Modifier) -> Unit
): @Composable (Modifier) -> Unit =
    when (val scope = LocalAdaptiveContentScope.current) {
        null -> throw IllegalArgumentException(
            "This may only be called from an adaptive content scope"
        )

        else -> when (scope.containerState.container) {
            null -> throw IllegalArgumentException(
                "Shared elements may only be used in non null containers"
            )
            // Allow shared elements in the primary or transient primary content only
            Adaptive.Container.Primary -> when {
                // Show a blank space for shared elements between the destinations
                scope.isInPreview && scope.isCurrentlyShared(key) -> EmptyElement
                // If previewing and it won't be shared, show the item as is
                scope.isInPreview -> sharedElement
                // Share the element
                else -> scope.rememberSharedContent(
                    key = key,
                    sharedElement = sharedElement
                )
            }
            // Share the element when in the transient container
            Adaptive.Container.TransientPrimary -> scope.rememberSharedContent(
                key = key,
                sharedElement = sharedElement
            )
            // In the secondary container use the element as is
            Adaptive.Container.Secondary -> sharedElement
        }
    }

internal val LocalAdaptiveContentScope = staticCompositionLocalOf<Adaptive.ContainerScope?> {
    null
}

private val EmptyElement: @Composable (Modifier) -> Unit = { modifier -> Box(modifier) }

internal val Adaptive.ContainerScope.isInPreview: Boolean
    get() = containerState.container == Adaptive.Container.Primary
            && containerState.adaptation == Adaptive.Adaptation.PrimaryToTransient
