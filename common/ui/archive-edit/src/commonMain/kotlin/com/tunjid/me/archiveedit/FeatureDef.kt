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

package com.tunjid.me.archiveedit

import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.data.di.DataComponent
import com.tunjid.me.feature.Feature
import com.tunjid.me.scaffold.di.ScaffoldComponent
import com.tunjid.me.scaffold.di.restoredState
import com.tunjid.treenav.strings.UrlRouteMatcher
import com.tunjid.treenav.strings.urlRouteMatcher
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass

object ArchiveEditFeature : Feature<ArchiveEditRoute, ArchiveEditMutator> {

    override val routeType: KClass<ArchiveEditRoute>
        get() = ArchiveEditRoute::class

    override val routeMatchers: List<UrlRouteMatcher<ArchiveEditRoute>> = listOf(
        urlRouteMatcher(
            routePattern = "archives/{kind}/{id}/edit",
            routeMapper = { (route: String, pathKeys: Map<String, String>) ->
                val archiveId = ArchiveId(pathKeys["id"] ?: "")
                val kind = ArchiveKind.values().firstOrNull { it.type == pathKeys["kind"] } ?: ArchiveKind.Articles
                ArchiveEditRoute(
                    id = route,
                    kind = kind,
                    archiveId = archiveId
                )
            }
        ),
        urlRouteMatcher(
            routePattern = "archives/{kind}/create",
            routeMapper = { (route, pathKeys) ->
                val kind = ArchiveKind.values().firstOrNull { it.type == pathKeys["kind"] } ?: ArchiveKind.Articles
                ArchiveEditRoute(
                    id = route,
                    kind = kind,
                    archiveId = null
                )
            }
        )
    )

    override fun mutator(
        scope: CoroutineScope,
        route: ArchiveEditRoute,
        scaffoldComponent: ScaffoldComponent,
        dataComponent: DataComponent
    ): ArchiveEditMutator = ActualArchiveEditMutator(
        scope = scope,
        route = route,
        initialState = scaffoldComponent.restoredState(route),
        archiveRepository = dataComponent.archiveRepository,
        authRepository = dataComponent.authRepository,
        uiStateFlow = scaffoldComponent.globalUiStateStream,
        lifecycleStateFlow = scaffoldComponent.lifecycleStateStream,
        permissionsFlow = scaffoldComponent.permissionsStream,
        onPermissionRequested = scaffoldComponent.permissionActions,
    )
}