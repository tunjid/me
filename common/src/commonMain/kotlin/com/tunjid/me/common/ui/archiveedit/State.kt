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

import com.tunjid.me.common.data.ByteSerializable
import com.tunjid.me.common.data.model.ArchiveKind
import com.tunjid.me.common.data.model.ArchiveUpsert
import com.tunjid.me.common.data.model.Descriptor
import com.tunjid.me.common.ui.common.ChipAction
import com.tunjid.mutator.Mutation
import kotlinx.serialization.Serializable

@Serializable
data class State(
    val isSignedIn: Boolean = false,
    val isSubmitting: Boolean = false,
    val navBarSize: Int,
    val kind: ArchiveKind,
    val upsert: ArchiveUpsert = ArchiveUpsert(),
    val chipsState: ChipsState = ChipsState(),
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

    data class ChipEdit(
        val chipAction: ChipAction,
        val descriptor: Descriptor,
    ): Action("ChipEdit")

    data class Submit(
        val kind: ArchiveKind,
        val upsert: ArchiveUpsert
    ): Action("Submit")
}

@Serializable
data class ChipsState(
    val categoryText: Descriptor.Category = Descriptor.Category(""),
    val tagText: Descriptor.Tag = Descriptor.Tag(""),
)