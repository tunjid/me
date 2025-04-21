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
import com.tunjid.me.common.data.ArchiveCategoryEntityQueries
import com.tunjid.me.common.data.ArchiveEntityQueries
import com.tunjid.me.common.data.ArchiveFileEntityQueries
import com.tunjid.me.common.data.ArchiveTagEntityQueries
import com.tunjid.me.common.data.SessionEntityQueries
import com.tunjid.me.common.data.UserEntityQueries
 import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.core.utilities.UriConverter
import com.tunjid.me.data.local.databaseDispatcher
import com.tunjid.me.data.network.ApiUrl
import com.tunjid.me.data.network.BaseUrl
import com.tunjid.me.data.network.KtorNetworkService
import com.tunjid.me.data.network.NetworkService
import com.tunjid.me.data.repository.ArchiveFileRepository
import com.tunjid.me.data.repository.ArchiveRepository
import com.tunjid.me.data.repository.AuthRepository
import com.tunjid.me.data.repository.DataStoreSavedStateRepository
import com.tunjid.me.data.repository.OfflineFirstArchiveFileRepository
import com.tunjid.me.data.repository.OfflineFirstArchiveRepository
import com.tunjid.me.data.repository.SavedStateRepository
import com.tunjid.me.data.repository.SessionCookieAuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope
import okio.Path

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class DataScope

class DataModule(
    internal val database: AppDatabase,
    internal val uriConverter: UriConverter,
    val savedStatePath: Path,
    val appScope: CoroutineScope,
    val byteSerializer: ByteSerializer,
)

/**
 * Wrapper for [UriConverter] to get around ksp compilation issues
 */
class UriConverterWrapper(uriConverter: UriConverter) : UriConverter by uriConverter

@DataScope
@Component
abstract class InjectedDataComponent(
    private val module: DataModule,
) {

    @Provides
    internal fun appUrl(): BaseUrl = ApiUrl

    @Provides
    internal fun json() = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    @DataScope
    @Provides
    fun appScope(): CoroutineScope = module.appScope

    @DataScope
    @Provides
    fun savedStatePath(): Path = module.savedStatePath

    @DataScope
    @Provides
    fun byteSerializer(): ByteSerializer = module.byteSerializer

    @DataScope
    @Provides
    fun uriConverter(): UriConverter = module.uriConverter

    @DataScope
    @Provides
    internal fun coroutineDispatcher(): CoroutineDispatcher = databaseDispatcher()

    @DataScope
    @Provides
    internal fun sessionEntityQueries(): SessionEntityQueries = module.database.sessionEntityQueries

    @DataScope
    @Provides
    internal fun userEntityQueries(): UserEntityQueries = module.database.userEntityQueries

    @DataScope
    @Provides
    internal fun archiveEntityQueries(): ArchiveEntityQueries = module.database.archiveEntityQueries

    @DataScope
    @Provides
    internal fun archiveTagEntityQueries(): ArchiveTagEntityQueries =
        module.database.archiveTagEntityQueries

    @DataScope
    @Provides
    internal fun archiveCategoryEntityQueries(): ArchiveCategoryEntityQueries =
        module.database.archiveCategoryEntityQueries

    @DataScope
    @Provides
    internal fun archiveFileEntityQueries(): ArchiveFileEntityQueries =
        module.database.archiveFileEntityQueries

    internal val KtorNetworkService.bind: NetworkService
        @DataScope
        @Provides get() = this

    internal val OfflineFirstArchiveRepository.bind: ArchiveRepository
        @DataScope
        @Provides get() = this

    internal val OfflineFirstArchiveFileRepository.bind: ArchiveFileRepository
        @DataScope
        @Provides get() = this

    internal val SessionCookieAuthRepository.bind: AuthRepository
        @DataScope
        @Provides get() = this

    val DataStoreSavedStateRepository.bind: SavedStateRepository
        @DataScope
        @Provides get() = this

    abstract val archiveRepository: ArchiveRepository

    abstract val archiveFileRepository: ArchiveFileRepository

    abstract val authRepository: AuthRepository
}