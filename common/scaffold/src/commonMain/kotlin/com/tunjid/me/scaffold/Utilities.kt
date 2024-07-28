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

@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package com.tunjid.me.scaffold

import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.current
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

infix fun Dp.countIf(condition: Boolean) = if (condition) this else 0.dp

/**
 * Updates [State] with whether it is the primary navigation container
 */
fun <State> StateFlow<MultiStackNav>.isInPrimaryNavMutations(
    route: Route,
    mutation: State.(Boolean) -> State,
): Flow<Mutation<State>> =
    map { route.id == it.current?.id }
        .distinctUntilChanged()
        .mapToMutation { isInPrimaryNav ->
            mutation(isInPrimaryNav)
        }

internal fun <T> adaptiveSpringSpec(visibilityThreshold: T) = spring(
    dampingRatio = 0.8f,
    stiffness = 600f,
    visibilityThreshold = visibilityThreshold
)