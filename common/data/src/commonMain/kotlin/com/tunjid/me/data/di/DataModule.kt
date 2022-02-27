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
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.data.local.*
import com.tunjid.me.data.network.*
import com.tunjid.me.data.network.models.item
import com.tunjid.me.data.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class DataModule(
    appScope: CoroutineScope,
    database: AppDatabase,
    internal val networkMonitor: NetworkMonitor
) {
    internal val archiveDao = SqlArchiveDao(
        database = database,
        dispatcher = databaseDispatcher(),
    )

    private val changeListDao: ChangeListDao = SqlChangeListDao(
        database = database,
        dispatcher = databaseDispatcher(),
    )

    private val sessionCookieDao: SessionCookieDao = SqlSessionCookieDao(
        database = database,
        dispatcher = databaseDispatcher(),
    )

    internal val networkService: NetworkService = KtorNetworkService(
        sessionCookieDao = sessionCookieDao
    )

    internal val archiveRepository = ReactiveArchiveRepository(
        networkService = networkService,
        networkMonitor = networkMonitor,
        dao = archiveDao
    )

    internal val authRepository = SessionCookieAuthRepository(
        networkService = networkService,
        dao = sessionCookieDao
    )

    private val changeListRepository = SqlChangeListRepository(
        appScope = appScope,
        networkService = networkService,
        changeListDao = changeListDao,
        archiveChangeListProcessor = archiveRepository
    )
}

class DataComponent(
    module: DataModule
) {
    internal val archiveDao: ArchiveDao = module.archiveDao
    internal val networkService: NetworkService = module.networkService

    val archiveRepository: ArchiveRepository = module.archiveRepository
    val authRepository: AuthRepository = module.authRepository
}

fun DataComponent.monitorServerEvents(
    scope: CoroutineScope,
    events: Flow<ModelEvent>
) {
    scope.launch {
        events
            .mapNotNull { event ->
                val kind = when (event.collection) {
                    ArchiveKind.Articles.type -> ArchiveKind.Articles
                    ArchiveKind.Talks.type -> ArchiveKind.Talks
                    ArchiveKind.Projects.type -> ArchiveKind.Projects
                    else -> null
                } ?: return@mapNotNull null

                exponentialBackoff(
                    initialDelay = 1_000,
                    maxDelay = 20_000,
                    times = 5,
                    default = null
                ) {
                    networkService.fetchArchive(
                        kind = kind, id = ArchiveId(
                            event.id
                        )
                    ).item()
                }
            }
            .collect {
                archiveDao.saveArchives(listOf(it))
            }
    }
}