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
import com.tunjid.me.core.model.ArchiveFileId
import com.tunjid.me.core.model.ArchiveFileQuery
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.scaffold.nav.NavigationAction
import com.tunjid.me.scaffold.nav.NavigationMutation
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.protobuf.ProtoNumber

sealed class Action(val key: String) {
    data class LoadAround(val query: ArchiveFileQuery) : Action("LoadAround")
    data class Navigate(
        override val navigationMutation: NavigationMutation
    ) : Action("Navigate"), NavigationAction
}

@Serializable
data class State(
//    @ProtoNumber(0)
//    val archiveFileId: ArchiveFileId,
//    @ProtoNumber(1)
    @ProtoNumber(2)
    val currentQuery: ArchiveFileQuery,
    @Transient
    val items: TiledList<ArchiveFileQuery, FileItem> = emptyTiledList(),
    @Transient
    val pagerOffset: Offset = Offset.Zero,
) : ByteSerializable


sealed class FileItem {
    data class PlaceHolder(
        val archiveFileId: ArchiveFileId,
        val url: String
    ) : FileItem()

    data class File(
        val archiveFile: ArchiveFile
    ) : FileItem()
}

val FileItem.archiveFileId: ArchiveFileId
    get() = when (this) {
        is FileItem.File -> archiveFile.id
        is FileItem.PlaceHolder -> archiveFileId
    }
val FileItem.key: String
    get() = when (this) {
        is FileItem.File -> archiveFile.id.value
        is FileItem.PlaceHolder -> archiveFileId.value
    }

val FileItem.url: String
    get() = when (this) {
        is FileItem.File -> archiveFile.url
        is FileItem.PlaceHolder -> url
    }