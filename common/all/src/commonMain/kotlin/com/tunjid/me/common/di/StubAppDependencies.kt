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

package com.tunjid.me.common.di

import com.tunjid.me.data.network.NetworkMonitor
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.mutator.Mutator
import com.tunjid.treenav.MultiStackNav

fun stubAppDependencies(
    nav: MultiStackNav = MultiStackNav(name = "App"),
    globalUI: UiState = UiState()
): AppDependencies = object : AppDependencies {

    override val byteSerializer: ByteSerializer
        get() = TODO("Not yet implemented")

    override val archiveRepository: ArchiveRepository
        get() = TODO("Not yet implemented")

    override val authRepository: AuthRepository
        get() = TODO("Not yet implemented")

    override val networkMonitor: NetworkMonitor
        get() = TODO("Not yet implemented")

    override val appMutator: AppMutator = AppState(
        ui = globalUI,
        nav = nav
    ).asAppMutator

    override fun <T : Mutator<*, *>> routeDependencies(route: AppRoute<T>): T =
        TODO("Not yet implemented")
}