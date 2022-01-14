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
@file:UseContextualSerialization

package com.tunjid.me.common.nav

import com.tunjid.me.common.data.ByteSerializable
import com.tunjid.me.common.data.archive.ArchiveKind
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.Route
import com.tunjid.treenav.StackNav
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization

/**
 * Nav supertype to avoid Kotlin serialization polymorphic compiler errors
 */
interface ByteSerializableRoute : Route, ByteSerializable

@Serializable
data class ByteSerializableNav(
    val currentIndex: Int,
    val indexHistory: List<Int> = listOf(),
    val kindToRoutes: Map<ArchiveKind, List<ByteSerializableRoute>> = mapOf()
) : ByteSerializable

val MultiStackNav.toByteSerializable
    get() = ByteSerializableNav(
        currentIndex = currentIndex,
        indexHistory = indexHistory,
        kindToRoutes = stacks.fold(mutableMapOf()) { map, stack ->
            val kind = ArchiveKind.values().first { it.name == stack.name }
            map[kind] = stack.routes.filterIsInstance<ByteSerializableRoute>()
            map
        }
    )

val ByteSerializableNav.toMultiStackNav
    get() = MultiStackNav(
        name = NavName,
        currentIndex = currentIndex,
        indexHistory = indexHistory,
        stacks = kindToRoutes
            .toList()
            .map { (kind, routes) ->
                StackNav(
                    name = kind.name,
                    routes = routes
                )
            }
    )