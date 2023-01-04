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

package com.tunjid.me.core.sync

import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ChangeListItem

data class SyncRequest(
    val model: String,
    val after: ChangeListItem?
)

fun ChangeListItem.toSyncRequest() = SyncRequest(
    model = model,
    after = this
)

/**
 * Interface marker for a class that is synchronized with a remote source. Syncing must not be
 * performed concurrently, and it is the [Synchronizer]'s responsibility to ensure this.
 */
fun interface Syncable {
    /**
     * Synchronizes the local database backing the repository with the network.
     * Returns if the sync was successful or not.
     */
    suspend fun syncWith(request: SyncRequest, onVersionUpdated: suspend (ChangeListItem) -> Unit)
}

fun String.changeListKey() = when (this) {
    ChangeListKey.User.model -> ChangeListKey.User
    ChangeListKey.Archive.Articles.model -> ChangeListKey.Archive.Articles
    ChangeListKey.Archive.Projects.model -> ChangeListKey.Archive.Projects
    ChangeListKey.Archive.Talks.model -> ChangeListKey.Archive.Talks
    ChangeListKey.ArchiveFile.Articles.model -> ChangeListKey.ArchiveFile.Articles
    ChangeListKey.ArchiveFile.Projects.model -> ChangeListKey.ArchiveFile.Projects
    ChangeListKey.ArchiveFile.Talks.model -> ChangeListKey.ArchiveFile.Talks
    else -> throw IllegalArgumentException("Unknown model")
}

/**
 * Keys used for key value storage
 */
sealed class ChangeListKey(
    val path: String,
    val model: String,
) {

    sealed class Archive(
        path: String,
        model: String,
        val kind: ArchiveKind,
    ) : ChangeListKey(path = path, model = model) {
        object Articles : Archive(
            path = "articles",
            kind = ArchiveKind.Articles,
            model = ArchiveKind.Articles.type,
        )

        object Projects : Archive(
            path = "projects",
            kind = ArchiveKind.Projects,
            model = ArchiveKind.Projects.type,
        )

        object Talks : Archive(
            path = "talks",
            kind = ArchiveKind.Talks,
            model = ArchiveKind.Talks.type,
        )
    }

    sealed class ArchiveFile(
        path: String,
        model: String,
        val kind: ArchiveKind,
    ) : ChangeListKey(path = path, model = model) {
        object Articles : ArchiveFile(
            path = "articlefiles",
            kind = ArchiveKind.Articles,
            model = "articlefiles",
        )

        object Projects : ArchiveFile(
            path = "projectfiles",
            kind = ArchiveKind.Projects,
            model = "projectfiles",
        )

        object Talks : ArchiveFile(
            path = "talkfiles",
            kind = ArchiveKind.Talks,
            model = "talkfiles",
        )
    }

    object User : ChangeListKey(
        path = "users",
        model = "user",
    )
}