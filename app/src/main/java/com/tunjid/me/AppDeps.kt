/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.me

import androidx.compose.runtime.staticCompositionLocalOf
import com.tunjid.me.globalui.UiState
import com.tunjid.me.globalui.globalUiMutator
import com.tunjid.me.nav.MultiStackNav
import com.tunjid.me.nav.navMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

interface AppDeps {
    val navMutator: Mutator<Mutation<MultiStackNav>, StateFlow<MultiStackNav>>
    val globalUiMutator: Mutator<Mutation<UiState>, StateFlow<UiState>>
}

fun createAppDependencies(scope: CoroutineScope) = object : AppDeps {
    override val navMutator: Mutator<Mutation<MultiStackNav>, StateFlow<MultiStackNav>> =
        navMutator(scope = scope)
    override val globalUiMutator: Mutator<Mutation<UiState>, StateFlow<UiState>> =
        globalUiMutator(scope = scope)
}

val AppDependencies = staticCompositionLocalOf<AppDeps> {
    object : AppDeps {
        override val navMutator: Mutator<Mutation<MultiStackNav>, StateFlow<MultiStackNav>>
            get() = TODO("Stub!")
        override val globalUiMutator: Mutator<Mutation<UiState>, StateFlow<UiState>>
            get() = TODO("Stub!")
    }
}