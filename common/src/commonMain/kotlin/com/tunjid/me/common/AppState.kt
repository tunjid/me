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
import com.tunjid.me.common.nav.Route
import com.tunjid.me.common.nav.pop
import com.tunjid.me.common.nav.push
import com.tunjid.me.common.nav.swap
import com.tunjid.me.common.ui.asNoOpStateFlowMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.Mutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

interface AppMutator : Mutator<AppAction, StateFlow<AppState>> {
    val navMutator: NavMutator
    val globalUiMutator: GlobalUiMutator
}

operator fun AppMutator.component1(): NavMutator = navMutator
operator fun AppMutator.component2(): GlobalUiMutator = globalUiMutator

val AppState.asAppMutator: AppMutator
    get() {
        val appState = this
        val baseMutator: Mutator<AppAction, StateFlow<AppState>> = appState.asNoOpStateFlowMutator()
        return object : AppMutator, Mutator<AppAction, StateFlow<AppState>> by baseMutator {
            override val navMutator: NavMutator = appState.nav.asNoOpStateFlowMutator()
            override val globalUiMutator: GlobalUiMutator = appState.ui.asNoOpStateFlowMutator()
        }
    }

fun <State : Any> Flow<AppAction>.consumeWith(
    appMutator: AppMutator
): Flow<Mutation<State>> =
    map { appMutator.accept(it) }
        .flatMapLatest { emptyFlow() }

data class AppState(
    val nav: MultiStackNav,
    val ui: UiState
)

sealed class AppAction {
    sealed class Nav : AppAction() {
        data class Push(val route: Route) : Nav()
        data class Swap(val route: Route) : Nav()
        object Pop : Nav()
    }
}

private val AppAction.Nav.mutation: Mutation<MultiStackNav>
    get() = Mutation {
        when (val action = this@mutation) {
            is AppAction.Nav.Push -> push(action.route)
            is AppAction.Nav.Swap -> swap(action.route)
            AppAction.Nav.Pop -> pop()
        }
    }

fun appMutator(
    scope: CoroutineScope,
    globalUiMutator: GlobalUiMutator,
    navMutator: NavMutator
): AppMutator = object : AppMutator {

    override val navMutator = navMutator

    override val globalUiMutator = globalUiMutator

    override val accept: (AppAction) -> Unit = { action ->
        when (action) {
            is AppAction.Nav -> navMutator.accept(action.mutation)
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
