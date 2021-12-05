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

package com.tunjid.me

import android.app.Application
import androidx.compose.runtime.staticCompositionLocalOf
import com.tunjid.me.globalui.UiState
import com.tunjid.me.nav.MultiStackNav
import com.tunjid.me.nav.StackNav
import com.tunjid.me.ui.archive.ArchiveKind
import com.tunjid.me.ui.archive.ArchiveRoute
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.coroutines.stateFlowMutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow

class App : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val appDeps by lazy { createAppDependencies() }

    override fun onCreate() {
        super.onCreate()
        appDeps
    }

    private fun createAppDependencies() = object : AppDeps {
        override val navMutator: Mutator<Mutation<MultiStackNav>, StateFlow<MultiStackNav>> =
            stateFlowMutator(
                scope = scope,
                initialState = MultiStackNav(
                    currentIndex = 0,
                    stacks = ArchiveKind.values().map { kind ->
                        StackNav(
                            name = kind.type,
                            routes = listOf(ArchiveRoute(kind = kind))
                        )
                    }
                ),
                transform = { it }
            )
        override val globalUiMutator: Mutator<Mutation<UiState>, StateFlow<UiState>> =
            stateFlowMutator(
                scope = scope,
                initialState = UiState(),
                transform = { it }
            )
    }
}

interface AppDeps {
    val navMutator: Mutator<Mutation<MultiStackNav>, StateFlow<MultiStackNav>>
    val globalUiMutator: Mutator<Mutation<UiState>, StateFlow<UiState>>
}

val AppDependencies = staticCompositionLocalOf<AppDeps> {
    object : AppDeps {
        override val navMutator: Mutator<Mutation<MultiStackNav>, StateFlow<MultiStackNav>>
            get() = TODO("Stub!")
        override val globalUiMutator: Mutator<Mutation<UiState>, StateFlow<UiState>>
            get() = TODO("Stub!")
    }
}