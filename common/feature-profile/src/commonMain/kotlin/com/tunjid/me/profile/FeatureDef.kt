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

package com.tunjid.me.profile

import com.tunjid.me.data.di.DataComponent
import com.tunjid.me.feature.Feature
import com.tunjid.me.scaffold.di.ScaffoldComponent
import com.tunjid.me.scaffold.di.restoredState
import com.tunjid.me.scaffold.nav.RouteParser
import com.tunjid.me.scaffold.nav.routeParser
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass

object ProfileFeature : Feature<ProfileRoute, ProfileMutator> {

    override val routeType: KClass<ProfileRoute>
        get() = ProfileRoute::class

    override val routeParsers: List<RouteParser<ProfileRoute>> = listOf(
        routeParser(
            pattern = "profile",
            routeMapper = { result ->
                ProfileRoute(
                    id = result.groupValues[0],
                )
            }
        )
    )

    override fun mutator(
        scope: CoroutineScope,
        route: ProfileRoute,
        scaffoldComponent: ScaffoldComponent,
        dataComponent: DataComponent
    ): ProfileMutator = profileMutator(
        scope = scope,
        route = route,
        initialState = scaffoldComponent.restoredState(route),
        authRepository = dataComponent.authRepository,
        lifecycleStateFlow = scaffoldComponent.lifecycleStateStream,
    )
}