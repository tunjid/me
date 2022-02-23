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

package com.tunjid.me.common.ui.archiveedit

import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.ui.ChipAction
import com.tunjid.mutator.Mutation
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val hasFetchedAuthStatus: Boolean = false,
    val isSignedIn: Boolean = false,
    val isSubmitting: Boolean = false,
    val navBarSize: Int,
    val kind: com.tunjid.me.core.model.ArchiveKind,
    val upsert: com.tunjid.me.core.model.ArchiveUpsert = com.tunjid.me.core.model.ArchiveUpsert(),
    val chipsState: ChipsState = ChipsState(),
    @Transient
    val messages: com.tunjid.me.core.model.MessageQueue = com.tunjid.me.core.model.MessageQueue(),
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

        data class Body(
            override val value: String
        ) : TextEdit()

        val mutation: Mutation<State>
            get() = when (this) {
                is Title -> Mutation { copy(upsert = upsert.copy(title = value)) }
                is Description -> Mutation { copy(upsert = upsert.copy(description = value)) }
                is Body -> Mutation { copy(upsert = upsert.copy(body = value)) }
            }
    }

    data class MessageConsumed(
        val message: com.tunjid.me.core.model.Message
    ) : Action("MessageConsumed")

    data class ChipEdit(
        val chipAction: ChipAction,
        val descriptor: com.tunjid.me.core.model.Descriptor,
    ) : Action("ChipEdit")

    sealed class Load : Action("Load") {
        data class InitialLoad(
            val kind: com.tunjid.me.core.model.ArchiveKind,
            val id: com.tunjid.me.core.model.ArchiveId
        ) : Load()

        data class Submit(
            val kind: com.tunjid.me.core.model.ArchiveKind,
            val upsert: com.tunjid.me.core.model.ArchiveUpsert
        ) : Load()
    }
}

@Serializable
data class ChipsState(
    val categoryText: com.tunjid.me.core.model.Descriptor.Category = com.tunjid.me.core.model.Descriptor.Category(""),
    val tagText: com.tunjid.me.core.model.Descriptor.Tag = com.tunjid.me.core.model.Descriptor.Tag(""),
)