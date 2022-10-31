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

package com.tunjid.me.archivedetail

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

object ArchiveDetailFeature : Feature<ArchiveDetailRoute, ArchiveDetailMutator> {

    override val routeType: KClass<ArchiveDetailRoute>
        get() = ArchiveDetailRoute::class

    override val routeMatchers: List<UrlRouteMatcher<ArchiveDetailRoute>> = listOf(
        urlRouteMatcher(
            routePattern = "archives/{kind}/{id}",
            routeMapper = { (route: String, pathKeys: Map<String, String>) ->
                val archiveId = ArchiveId(pathKeys["id"] ?: "")
                val kind = ArchiveKind.values().firstOrNull { it.type == pathKeys["kind"] } ?: ArchiveKind.Articles
                ArchiveDetailRoute(
                    id = route,
                    kind = kind,
                    archiveId = archiveId
                )
            }
        )
    )

    override fun mutator(
        scope: CoroutineScope,
        route: ArchiveDetailRoute,
        scaffoldComponent: ScaffoldComponent,
        dataComponent: DataComponent
    ): ArchiveDetailMutator = ActualArchiveDetailMutator(
        scope = scope,
        route = route,
        initialState = scaffoldComponent.restoredState(route),
        archiveRepository = dataComponent.archiveRepository,
        authRepository = dataComponent.authRepository,
        uiStateFlow = scaffoldComponent.globalUiStateStream,
        lifecycleStateFlow = scaffoldComponent.lifecycleStateStream,
        navActions = scaffoldComponent.navActions,
    )
}