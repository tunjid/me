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

package com.tunjid.me.feature.archivefiles

import com.tunjid.me.core.model.ArchiveFile
import com.tunjid.me.core.model.ArchiveFileId
import com.tunjid.me.core.model.ArchiveFileQuery
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.utilities.Uri
import com.tunjid.me.scaffold.navigation.NavigationAction
import com.tunjid.me.scaffold.navigation.NavigationMutation
import com.tunjid.me.scaffold.permissions.Permission
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.routeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.protobuf.ProtoNumber


enum class DragLocation {
    Inactive, Outside, Inside
}

sealed class UploadInfo {
    data object None : UploadInfo()

    data class Message(
        val message: String
    ) : UploadInfo()

    data class Progress(
        val message: String,
        val progress: Float
    ) : UploadInfo()
}

@Serializable
data class State(
    @ProtoNumber(4)
    val archiveId: ArchiveId,
    @ProtoNumber(0)
    val isSignedIn: Boolean = false,
    @ProtoNumber(1)
    val hasFetchedAuthStatus: Boolean = false,
    @ProtoNumber(2)
    val isInPrimaryNav: Boolean = true,
    @ProtoNumber(3)
    val hasStoragePermissions: Boolean = false,
    @ProtoNumber(5)
    val currentQuery: ArchiveFileQuery,
    @Transient
    val dndEnabled: Boolean = false,
    @Transient
    val fileType: FileType = FileType.Image,
    @Transient
    val uploadInfo: UploadInfo = UploadInfo.None,
    @Transient
    val dragLocation: DragLocation = DragLocation.Inactive,
    @Transient
    val items: TiledList<ArchiveFileQuery, FileItem> = emptyTiledList(),
) : ByteSerializable

fun State.startQuery() = ArchiveFileQuery(
    archiveId = archiveId,
    mimeTypes = fileType.mimeTypes
)

sealed class Action(val key: String) {
    data class Drag(val location: DragLocation) : Action("Drag")

    data class Drop(val uris: List<Uri>) : Action("Drop")

    data class RequestPermission(val permission: Permission) : Action("RequestPermission")

    sealed class Fetch : Action("Fetch") {
        data class LoadAround(val query: ArchiveFileQuery) : Fetch()

        data class ColumnSizeChanged(val size: Int) : Fetch()
    }

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data class Gallery(
            val archiveId: ArchiveId,
            val fileId: ArchiveFileId,
            val thumbnail: String?
        ) : Navigate() {
            override val navigationMutation: NavigationMutation = {
                navState.push(
                    routeString(
                        path = "/archive/${archiveId.value}/gallery",
                        queryParams = mapOf(
                            "fileId" to listOf(fileId.value),
                            "url" to listOfNotNull(thumbnail)
                        )
                    ).toRoute
                )
            }
        }
    }
}

sealed class FileItem {
    data class PlaceHolder(
        val url: String
    ) : FileItem()

    data class File(
        val archiveFile: ArchiveFile
    ) : FileItem()
}

val FileItem.key: String
    get() = when (this) {
        is FileItem.File -> url
        is FileItem.PlaceHolder -> url
    }

val FileItem.url: String
    get() = when (this) {
        is FileItem.File -> archiveFile.url
        is FileItem.PlaceHolder -> url
    }
