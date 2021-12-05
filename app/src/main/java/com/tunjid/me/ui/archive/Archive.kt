/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.me.ui.archive

import java.util.*

enum class ArchiveKind(val type: String) {
    Articles("articles"),
    Projects("projects"),
    Talks("talks"),
}

interface ArchiveLike {
    val key: String
    val link: String
    val title: String
    val body: String
    val description: String
    val thumbnail: String?
    val author: User
    val created: Date
    val spanCount: Int?
    val tags: Array<String>
    val categories: Array<String>
    val kind: ArchiveKind
}

interface User {
    val id: String
    val firstName: String
    val lastName: String
    val fullName: String
    val imageUrl: String
};