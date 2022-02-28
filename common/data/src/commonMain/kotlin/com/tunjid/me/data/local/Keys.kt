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

package com.tunjid.me.data.local

import com.tunjid.me.core.model.ArchiveKind

/**
 * Keys used for key value storage
 */
internal sealed class Keys(val key: String) {
    object SessionCookieId : Keys(key = "session-cookie")

    sealed class ChangeList(
        key: String, val path: String
    ) : Keys(key = key) {

        sealed class Archive(
            key: String,
            path: String,
            val kind: ArchiveKind,
        ) : ChangeList(key = key, path = path) {
            object Articles : Archive(
                key = "articles-change-list",
                path = "articles",
                kind = ArchiveKind.Articles,
            )

            object Projects : Archive(
                key = "projects-change-list",
                path = "projects",
                kind = ArchiveKind.Projects,
            )

            object Talks : Archive(
                key = "talks-change-list",
                path = "talks",
                kind = ArchiveKind.Talks,
            )
        }

        object User : ChangeList(
            key = "users-change-list",
            path = "users",
        )
    }
}