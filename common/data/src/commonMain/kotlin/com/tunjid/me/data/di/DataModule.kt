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

import com.tunjid.me.common.data.*
import com.tunjid.me.core.di.SingletonScope
import com.tunjid.me.core.utilities.UriConverter
import com.tunjid.me.data.local.databaseDispatcher
import com.tunjid.me.data.network.ApiUrl
import com.tunjid.me.data.network.BaseUrl
import com.tunjid.me.data.network.KtorNetworkService
import com.tunjid.me.data.network.NetworkService
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.data.repository.OfflineFirstArchiveRepository
import com.tunjid.me.data.repository.SessionCookieAuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

class DataModule(
    internal val database: AppDatabase,
    internal val uriConverter: UriConverter
) {
    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    private val networkService: NetworkService = KtorNetworkService(
        json = json,
        baseUrl = ApiUrl,
        sessionEntityQueries = database.sessionEntityQueries,
        dispatcher = databaseDispatcher(),
    )

    internal val archiveRepository = OfflineFirstArchiveRepository(
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

@SingletonScope
@Component
abstract class InjectedDataComponent(
    private val module: DataModule
) {

    @Provides
    internal fun appUrl(): BaseUrl = ApiUrl

    @Provides
    internal fun json() = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    @SingletonScope
    @Provides
    internal fun uriConverter(): UriConverter = module.uriConverter

    @SingletonScope
    @Provides
    internal fun coroutineDispatcher(): CoroutineDispatcher = databaseDispatcher()

    @SingletonScope
    @Provides
    internal fun sessionEntityQueries(): SessionEntityQueries = module.database.sessionEntityQueries

    @SingletonScope
    @Provides
    internal fun userEntityQueries(): UserEntityQueries = module.database.userEntityQueries

    @SingletonScope
    @Provides
    internal fun archiveEntityQueries(): ArchiveEntityQueries = module.database.archiveEntityQueries

    @SingletonScope
    @Provides
    internal fun archiveTagEntityQueries(): ArchiveTagEntityQueries = module.database.archiveTagEntityQueries

    @SingletonScope
    @Provides
    internal fun archiveCategoryEntityQueries(): ArchiveCategoryEntityQueries =
        module.database.archiveCategoryEntityQueries

    internal val KtorNetworkService.bind: NetworkService
        @SingletonScope
        @Provides get() = this

    internal val OfflineFirstArchiveRepository.bind: ArchiveRepository
        @SingletonScope
        @Provides get() = this

    internal val SessionCookieAuthRepository.bind: AuthRepository
        @SingletonScope
        @Provides get() = this

    abstract val archiveRepository: ArchiveRepository

    abstract val authRepository: AuthRepository
}