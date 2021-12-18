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

package com.tunjid.me.nav

import com.tunjid.me.data.archive.ArchiveKind
import com.tunjid.me.data.archive.ArchiveQuery
import com.tunjid.me.ui.archive.ArchiveRoute
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

fun navMutator(scope: CoroutineScope): Mutator<Mutation<MultiStackNav>, StateFlow<MultiStackNav>> =
    stateFlowMutator(
        scope = scope,
        initialState = MultiStackNav(
            currentIndex = 0,
            stacks = ArchiveKind.values().map { kind ->
                StackNav(
                    name = kind.type,
                    routes = listOf(ArchiveRoute(query = ArchiveQuery(kind = kind)))
                )
            }
        ),
        transform = { it }
    )
