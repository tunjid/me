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

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.tunjid.me.core.model.*
import com.tunjid.me.core.ui.ChipAction
import com.tunjid.me.core.ui.ChipInfo
import com.tunjid.me.core.ui.ChipKind
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.utilities.LocalUri
import com.tunjid.me.core.utilities.Uri
import com.tunjid.me.scaffold.nav.NavMutation
import com.tunjid.me.scaffold.permissions.Permission
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.mutation
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.protobuf.ProtoNumber

enum class DragLocation {
    None, InWindow, InThumbnail
}

@Serializable
data class State(
    @ProtoNumber(1)
    val hasFetchedAuthStatus: Boolean = false,
    @ProtoNumber(2)
    val isSignedIn: Boolean = false,
    @ProtoNumber(3)
    val isEditing: Boolean = true,
    @ProtoNumber(4)
    val isSubmitting: Boolean = false,
    @Transient
    val hasStoragePermissions: Boolean = false,
    @ProtoNumber(6)
    val navBarSize: Int,
    @ProtoNumber(7)
    val kind: ArchiveKind,
    @Transient
    val thumbnail: String? = null,
    @Transient
    val body: TextFieldValue = TextFieldValue(),
    @Transient
    val upsert: ArchiveUpsert = ArchiveUpsert(),
    @ProtoNumber(10)
    val chipsState: ChipsState = ChipsState(),
    @Transient
    val toUpload: LocalUri? = null,
    @Transient
    val dragLocation: DragLocation = DragLocation.None,
    @Transient
    val messages: MessageQueue = MessageQueue(),
) : ByteSerializable

sealed class Action(val key: String) {
    sealed class TextEdit : Action("TextEdit") {

        data class Title(
             val value: String
        ) : TextEdit()

        data class Description(
             val value: String
        ) : TextEdit()

        data class VideoUrl(
             val value: String
        ) : TextEdit()

        sealed class Body : TextEdit() {
            data class Edit(
                val textFieldValue: TextFieldValue,
            ) : TextEdit()

            data class CursorIndex(
                val index: Int,
            ) : TextEdit()

            data class ImageDrop(
                val index: Int,
                val uri: Uri,
            ) : TextEdit()
        }

        val mutation: Mutation<State>
            get() = when (this) {
                is Title -> mutation { copy(upsert = upsert.copy(title = value)) }
                is Description -> mutation { copy(upsert = upsert.copy(description = value)) }
                is VideoUrl -> mutation { copy(upsert = upsert.copy(videoUrl = value)) }
                is Body.Edit -> mutation {
                    copy(
                        body = textFieldValue,
                        upsert = upsert.copy(body = textFieldValue.text)
                    )
                }
                is Body.CursorIndex -> mutation {
                    copy(body = body.copy(selection = TextRange(index = index)))
                }
                is Body.ImageDrop -> mutation {
                    val existingText = body.text
                    val startSubstring = existingText.substring(
                        startIndex = 0,
                        endIndex = index
                    )
                    val imageMarkDown = "\n![image](${uri.path})\n"
                    val endSubstring = existingText.substring(
                        startIndex = index,
                        endIndex = existingText.length
                    )
                    val text = startSubstring + imageMarkDown + endSubstring
                    copy(
                        body = TextFieldValue(
                            text = text,
                            selection = TextRange(index = index + imageMarkDown.length)
                        ),
                        upsert = upsert.copy(body = text)
                    )
                }
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
            val headerPhoto: LocalUri? = null
        ) : Load()
    }

    sealed class Drag : Action("Drag") {
        data class Window(val inside: Boolean) : Drag()
        data class Thumbnail(val inside: Boolean) : Drag()
    }

    data class Drop(val uris: List<Uri>) : Action("Drop")

    data class RequestPermission(val permission: Permission) : Action("RequestPermission")

    data class Navigate(val navMutation: NavMutation) : Action(key = "Navigate")
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
