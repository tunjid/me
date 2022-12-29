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

import com.tunjid.me.core.model.*
import com.tunjid.me.core.ui.ChipAction
import com.tunjid.me.core.ui.ChipInfo
import com.tunjid.me.core.ui.ChipKind
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.utilities.Uri
import com.tunjid.me.scaffold.permissions.Permission
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.mutation
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

enum class DragStatus {
    None, InWindow, InThumbnail
}

@Serializable
data class State(
    val hasFetchedAuthStatus: Boolean = false,
    val isSignedIn: Boolean = false,
    val isEditing: Boolean = true,
    val isSubmitting: Boolean = false,
    val hasStoragePermissions: Boolean = false,
    val navBarSize: Int,
    val kind: ArchiveKind,
    val thumbnail: String? = null,
    val upsert: ArchiveUpsert = ArchiveUpsert(),
    val chipsState: ChipsState = ChipsState(),
    @Transient
    val toUpload: Uri? = null,
    @Transient
    val dragStatus: DragStatus = DragStatus.None,
    @Transient
    val messages: MessageQueue = MessageQueue(),
) : ByteSerializable

sealed class Action(val key: String) {
    sealed class TextEdit : Action("TextEdit") {
        abstract val value: String

        data class Title(
            override val value: String
        ) : TextEdit()

        data class Description(
            override val value: String
        ) : TextEdit()

        data class VideoUrl(
            override val value: String
        ) : TextEdit()

        data class Body(
            override val value: String
        ) : TextEdit()

        val mutation: Mutation<State>
            get() = when (this) {
                is Title -> mutation { copy(upsert = upsert.copy(title = value)) }
                is Description -> mutation { copy(upsert = upsert.copy(description = value)) }
                is VideoUrl -> mutation { copy(upsert = upsert.copy(videoUrl = value)) }
                is Body -> mutation { copy(upsert = upsert.copy(body = value)) }
            }
    }

    object ToggleEditView : Action("ToggleEditView")

    data class MessageConsumed(
        val message: Message
    ) : Action("MessageConsumed")

    data class ChipEdit(
        val chipAction: ChipAction,
        val descriptor: Descriptor,
    ) : Action("ChipEdit")

    sealed class Load : Action("Load") {
        data class InitialLoad(
            val kind: ArchiveKind,
            val id: ArchiveId
        ) : Load()

        data class Submit(
            val kind: ArchiveKind,
            val upsert: ArchiveUpsert,
            val headerPhoto: Uri? = null
        ) : Load()
    }

    sealed class Drag : Action("Drag") {
        data class Window(val inside: Boolean) : Drag()
        data class Thumbnail(val inside: Boolean) : Drag()
    }

    data class Drop(val uris: List<Uri>) : Action("Drop")

    data class RequestPermission(val permission: Permission) : Action("RequestPermission")
}

@Serializable
data class ChipsState(
    val categoryText: Descriptor.Category = Descriptor.Category(""),
    val tagText: Descriptor.Tag = Descriptor.Tag(""),
)

inline fun <reified T : Descriptor> ArchiveUpsert.descriptorChips() =
    when (T::class) {
        Descriptor.Tag::class -> tags
        Descriptor.Category::class -> categories
        else -> throw IllegalArgumentException("Invalid descriptor class: ${T::class.qualifiedName}")
    }.map {
        ChipInfo(
            text = it.value,
            kind = ChipKind.Assist()
        )
    }
