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

import com.tunjid.me.archivedetail.di.ArchiveDetailNavigationComponent
import com.tunjid.me.archiveedit.di.ArchiveEditNavigationComponent
import com.tunjid.me.core.di.SingletonScope
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.core.utilities.DelegatingByteSerializer
import com.tunjid.me.feature.archivelist.di.ArchiveListNavigationComponent
import com.tunjid.me.profile.di.ProfileNavigationComponent
import com.tunjid.me.scaffold.di.SavedStateType
import com.tunjid.me.scaffold.nav.AppRoute
import com.tunjid.me.settings.di.SettingsNavigationComponent
import com.tunjid.me.signin.di.SignInNavigationComponent
import com.tunjid.treenav.strings.UrlRouteMatcher
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.protobuf.ProtoBuf
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@SingletonScope
@Component
abstract class AppRouteComponent(
    @Component val archiveListNavigationComponent: ArchiveListNavigationComponent,
    @Component val archiveDetailNavigationComponent: ArchiveDetailNavigationComponent,
    @Component val archiveEditNavigationComponent: ArchiveEditNavigationComponent,
    @Component val profileNavigationComponent: ProfileNavigationComponent,
    @Component val settingsNavigationComponent: SettingsNavigationComponent,
    @Component val signInNavigationComponent: SignInNavigationComponent,
) {
    abstract val allScreenStatePolymorphic: Set<SavedStateType>

    abstract val allUrlRouteMatchers: Set<UrlRouteMatcher<AppRoute>>

    abstract val byteSerializer: ByteSerializer

    @Provides
    fun byteSerializer(): ByteSerializer = DelegatingByteSerializer(
        format = ProtoBuf {
            serializersModule = SerializersModule {
                polymorphic(ByteSerializable::class) {
                    allScreenStatePolymorphic.forEach { it.apply(this) }
                }
            }
        }
    )
}