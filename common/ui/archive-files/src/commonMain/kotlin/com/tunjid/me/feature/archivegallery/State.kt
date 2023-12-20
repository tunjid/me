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

package com.tunjid.me.feature.archivegallery

import androidx.compose.ui.geometry.Offset
import com.tunjid.me.core.model.ArchiveFile
import com.tunjid.me.core.model.ArchiveFileQuery
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.scaffold.navigation.NavigationAction
import com.tunjid.me.scaffold.navigation.NavigationMutation
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.treenav.pop
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.protobuf.ProtoNumber

sealed class Action(val key: String) {
    data class LoadAround(val query: ArchiveFileQuery) : Action("LoadAround")
    sealed class Navigate: Action(key = "Navigate"), NavigationAction {
        data object Pop : Navigate() {
            override val navigationMutation: NavigationMutation = {
                navState.pop()
            }
        }
    }
}

@Serializable
data class State(
//    @ProtoNumber(0)
//    val archiveFileId: ArchiveFileId,
//    @ProtoNumber(1)
    @ProtoNumber(2)
    val currentQuery: ArchiveFileQuery,
    @Transient
    val items: TiledList<ArchiveFileQuery, GalleryItem> = emptyTiledList(),
    @Transient
    val pagerOffset: Offset = Offset.Zero,
) : ByteSerializable


sealed class GalleryItem {
    data class PlaceHolder(
        val url: String
    ) : GalleryItem()

    data class File(
        val archiveFile: ArchiveFile
    ) : GalleryItem()
}

val GalleryItem.key: String
    get() = when (this) {
        is GalleryItem.File -> url
        is GalleryItem.PlaceHolder -> url
    }

val GalleryItem.url: String
    get() = when (this) {
        is GalleryItem.File -> archiveFile.url
        is GalleryItem.PlaceHolder -> url
    }