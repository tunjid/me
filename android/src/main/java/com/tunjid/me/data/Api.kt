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

package com.tunjid.me.data

import com.tunjid.me.data.archive.Archive
import com.tunjid.me.data.archive.ArchiveKind
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface Api {
    @GET("api/{kind}")
    suspend fun fetchArchives(
        @Path("kind") kind: ArchiveKind,
        @QueryMap options: Map<String, String> = mapOf(),
        @Query("tag") tags: List<String> = listOf(),
        @Query("category") categories: List<String> = listOf(),
    ): List<Archive>

    @GET("api/{kind}/{id}")
    suspend fun fetchArchive(
        @Path("kind") kind: ArchiveKind,
        @Path("id") id: String,
    ): Archive
}
