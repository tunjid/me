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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.tunjid.me.core.model.Archive
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.ui.ChipInfo
import com.tunjid.me.core.ui.ChipKind
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.me.scaffold.navigation.NavigationAction
import com.tunjid.me.scaffold.navigation.NavigationMutation
import com.tunjid.treenav.pop
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.routeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.protobuf.ProtoNumber

sealed class Action(val key: String) {

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data object Pop : Navigate() {
            override val navigationMutation: NavigationMutation = {
                navState.pop()
            }
        }

        data class Edit(
            val kind: ArchiveKind,
            val archiveId: ArchiveId,
            val thumbnail: String?
        ) : Navigate() {
            override val navigationMutation: NavigationMutation = {
                navState.push(
                    routeString(
                        path = "/archives/${kind.type}/${archiveId.value}/edit",
                        queryParams = mapOf(
                            "thumbnail" to listOfNotNull(thumbnail)
                        )
                    ).toRoute
                )
            }
        }

        data class Files(
            val kind: ArchiveKind,
            val archiveId: ArchiveId,
            val thumbnail: String?
        ) : Navigate() {
            override val navigationMutation: NavigationMutation = {
                navState.push(
                    routeString(
                        path = "/archives/${kind.type}/${archiveId.value}/files",
                        queryParams = mapOf(
                            "url" to listOfNotNull(thumbnail)
                        )
                    ).toRoute
                )
            }
        }
    }
}

@Serializable
data class State(
    @ProtoNumber(1)
    val hasFetchedAuthStatus: Boolean = false,
    @ProtoNumber(2)
    val signedInUserId: com.tunjid.me.core.model.UserId? = null,
    @ProtoNumber(4)
    val wasDeleted: Boolean = false,
    @ProtoNumber(5)
    val kind: ArchiveKind,
    @ProtoNumber(6)
    val isInPrimaryNav: Boolean = false,
    @ProtoNumber(7)
    val routeThumbnailUrl: String? = null,
    // Read this from the DB
    @Transient
    val archive: Archive? = null,
    @Transient
    val hasSecondaryPanel: Boolean = false,
) : ByteSerializable

val State.canEdit: Boolean
    get() = signedInUserId != null && signedInUserId == archive?.author?.id

val State.headerThumbnail
    get() = archive?.thumbnail ?: routeThumbnailUrl
val State.sharedElementKey
    get() = thumbnailSharedElementKey(headerThumbnail)

@Composable
inline fun <reified T : Descriptor> State.descriptorChips() =
    when (archive) {
        null -> listOf()
        else -> when (T::class) {
            Descriptor.Tag::class -> archive.tags.map { it to MaterialTheme.colorScheme.tertiaryContainer }
            Descriptor.Category::class -> archive.categories.map { it to MaterialTheme.colorScheme.secondaryContainer }
            else -> throw IllegalArgumentException("Invalid descriptor class: ${T::class.qualifiedName}")
        }.map { (descriptor, tint) ->
            ChipInfo(
                text = descriptor.value,
                kind = ChipKind.Assist(tint = tint)
            )
        }
    }
