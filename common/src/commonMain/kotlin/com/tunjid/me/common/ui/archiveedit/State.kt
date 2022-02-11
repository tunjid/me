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
import com.tunjid.me.common.data.model.ArchiveQuery
import com.tunjid.me.common.data.model.Descriptor
import com.tunjid.me.common.ui.common.ChipAction
import com.tunjid.mutator.Mutation
import kotlinx.serialization.Serializable

@Serializable
data class State(
    val archiveId: String? = null,
    val isSignedIn: Boolean = false,
    val navBarSize: Int,
    val title: String = "",
    val body: String = "",
    val description: String = "",
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
                is Title -> Mutation { copy(title = value) }
                is Description -> Mutation { copy(description = value) }
                is Body -> Mutation { copy(body = value) }
            }
    }

    data class ChipEdit(
        val chipAction: ChipAction,
        val descriptor: Descriptor,
    ): Action("ChipEdit")
}

@Serializable
data class ChipsState(
    val categories: List<Descriptor.Category> = listOf(),
    val tags: List<Descriptor.Tag> = listOf(),
    val categoryText: Descriptor.Category = Descriptor.Category(""),
    val tagText: Descriptor.Tag = Descriptor.Tag(""),
)