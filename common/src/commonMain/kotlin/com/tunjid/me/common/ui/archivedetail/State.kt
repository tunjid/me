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

package com.tunjid.me.common.ui.archivedetail

import com.tunjid.me.common.data.ByteSerializable
import com.tunjid.me.common.data.model.Archive
import com.tunjid.me.common.data.model.ArchiveKind
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val signedInUserId: String? = null,
    val navBarSize: Int,
    val kind: ArchiveKind,
    // Read this from the DB
    @Transient
    val archive: Archive? = null,
) : ByteSerializable

val State.canEdit: Boolean get() = signedInUserId != null && signedInUserId == archive?.author?.id