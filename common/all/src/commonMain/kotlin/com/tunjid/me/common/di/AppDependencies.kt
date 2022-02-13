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

import androidx.compose.runtime.staticCompositionLocalOf
import com.tunjid.me.common.data.AppDatabase
import com.tunjid.me.common.data.ByteSerializer
import com.tunjid.me.common.data.local.ArchiveDao
import com.tunjid.me.common.data.local.SessionCookieDao
import com.tunjid.me.common.data.network.NetworkMonitor
import com.tunjid.me.common.data.repository.ArchiveRepository
import com.tunjid.me.common.data.repository.AuthRepository
import com.tunjid.me.common.globalui.UiState
import com.tunjid.me.common.nav.AppRoute
import kotlinx.coroutines.CoroutineScope

interface AppDependencies {
    val appDatabase: AppDatabase
    val appMutator: AppMutator
    val networkMonitor: NetworkMonitor
    val byteSerializer: ByteSerializer
    val archiveRepository: ArchiveRepository
    val authRepository: AuthRepository
    val archiveDao: ArchiveDao
    val sessionCookieDao: SessionCookieDao
    fun <T> routeDependencies(route: AppRoute<T>): T
}

val LocalAppDependencies = staticCompositionLocalOf {
    stubAppDependencies()
}
