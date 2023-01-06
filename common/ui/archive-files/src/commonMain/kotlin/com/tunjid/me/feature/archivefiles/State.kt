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
import com.tunjid.me.core.model.MessageQueue
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.utilities.Uri
import com.tunjid.me.scaffold.nav.NavMutation
import com.tunjid.me.scaffold.permissions.Permission
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


enum class DragLocation {
    Outside, Inside
}

@Serializable
data class State(
    val isSignedIn: Boolean = false,
    val hasFetchedAuthStatus: Boolean = false,
    val isMainContent: Boolean = true,
    val hasStoragePermissions: Boolean = false,
    @Transient
    val uploadProgress: Float? = null,
    @Transient
    val messages: MessageQueue = MessageQueue(),
    @Transient
    val dragLocation: DragLocation = DragLocation.Outside,
    @Transient
    val files: List<ArchiveFile> = emptyList(),
) : ByteSerializable


sealed class Action(val key: String) {
    data class Drag(val location: DragLocation) : Action("Drag")

    data class Drop(val uris: List<Uri>) : Action("Drop")

    data class RequestPermission(val permission: Permission) : Action("RequestPermission")
}
