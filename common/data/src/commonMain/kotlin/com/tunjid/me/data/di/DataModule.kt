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

package com.tunjid.me.data.di

import com.tunjid.me.common.data.AppDatabase
import com.tunjid.me.core.utilities.UriConverter
import com.tunjid.me.data.local.databaseDispatcher
import com.tunjid.me.data.network.KtorNetworkService
import com.tunjid.me.data.network.NetworkMonitor
import com.tunjid.me.data.network.NetworkService
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.data.repository.ReactiveArchiveRepository
import com.tunjid.me.data.repository.SessionCookieAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

class DataModule(
    appScope: CoroutineScope,
    database: AppDatabase,
    uriConverter: UriConverter,
    internal val networkMonitor: NetworkMonitor
) {
    internal val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    private val networkService: NetworkService = KtorNetworkService(
        json = json,
        sessionEntityQueries = database.sessionEntityQueries,
        dispatcher = databaseDispatcher(),
    )

    internal val archiveRepository = ReactiveArchiveRepository(
        uriConverter = uriConverter,
        networkService = networkService,
        archiveEntityQueries = database.archiveEntityQueries,
        archiveTagQueries = database.archiveTagEntityQueries,
        archiveCategoryQueries = database.archiveCategoryEntityQueries,
        archiveAuthorQueries = database.userEntityQueries,
        dispatcher = databaseDispatcher(),
    )

    internal val authRepository = SessionCookieAuthRepository(
        networkService = networkService,
        userEntityQueries = database.userEntityQueries,
        sessionEntityQueries = database.sessionEntityQueries,
        dispatcher = databaseDispatcher(),
    )
}

class DataComponent(
    module: DataModule
) {
    val archiveRepository: ArchiveRepository = module.archiveRepository
    val authRepository: AuthRepository = module.authRepository
}
