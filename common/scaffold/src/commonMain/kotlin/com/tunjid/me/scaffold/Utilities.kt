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

package com.tunjid.me.scaffold

import androidx.compose.animation.core.spring
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.scaffold.nav.NavState
import com.tunjid.me.scaffold.nav.primaryRoute
import com.tunjid.mutator.Mutation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Updates [State] with whether it is the main navigation content
 */
fun <State> StateFlow<NavState>.isInPrimaryNavMutations(
    route: AppRoute,
    mutation: State.(Boolean) -> State,
): Flow<Mutation<State>> = map { route.id == it.primaryRoute.id }
    .distinctUntilChanged()
    .map { isInPrimaryNav ->
        com.tunjid.mutator.mutation { mutation(isInPrimaryNav) }
    }

internal fun <T> adaptiveSpringSpec(visibilityThreshold: T) = spring(
    dampingRatio = 0.8f,
    stiffness = 600f,
    visibilityThreshold = visibilityThreshold
)