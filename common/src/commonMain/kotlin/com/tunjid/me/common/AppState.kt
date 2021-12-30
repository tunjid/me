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

package com.tunjid.me.common

import com.tunjid.me.common.globalui.GlobalUiMutator
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.nav.MultiStackNav
import com.tunjid.me.common.nav.NavMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

interface AppMutator : Mutator<AppAction<*>, StateFlow<AppState>>{
    val  navMutator: NavMutator
    val globalUiMutator: GlobalUiMutator
    operator fun component1(): NavMutator = navMutator
    operator fun component2(): GlobalUiMutator = globalUiMutator
}

data class AppState(
    val nav: MultiStackNav,
    val ui: UiState
)

sealed class AppAction<T : Any>(mutation: T.() -> T) {
    val mutation: Mutation<T> = Mutation(mutation)

    class NavMutation(
        mutation: MultiStackNav.() -> MultiStackNav
    ) : AppAction<MultiStackNav>(mutation)

    class UiMutation(
        mutation: UiState.() -> UiState
    ) : AppAction<UiState>(mutation)

}

fun appMutator(
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    globalUiMutator: GlobalUiMutator,
    navMutator: NavMutator
): AppMutator = object : AppMutator {

    override val navMutator = navMutator

    override val globalUiMutator = globalUiMutator

    override val accept: (AppAction<*>) -> Unit = { action ->
        when (action) {
            is AppAction.NavMutation -> navMutator.accept(action.mutation)
            is AppAction.UiMutation -> globalUiMutator.accept(action.mutation)
        }
    }
    override val state: StateFlow<AppState> = combine(
        navMutator.state,
        globalUiMutator.state,
        ::AppState
    ).stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = AppState(
            nav = navMutator.state.value,
            ui = globalUiMutator.state.value,
        )
    )
}
