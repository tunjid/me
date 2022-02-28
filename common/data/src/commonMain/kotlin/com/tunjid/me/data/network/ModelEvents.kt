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

package com.tunjid.me.data.network

import com.tunjid.me.core.model.ChangeListItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// TODO: Use the Json from the DataModule
private val json = Json {
    explicitNulls = false
    ignoreUnknownKeys = true
}

fun List<String>.toChangeListItem(
    event: String
): ChangeListItem? {
    if (event != ModelChangedEvent) return null
    val rawJson = firstOrNull()  ?: return null
    return try {
        json.decodeFromString<ChangeListItem>(rawJson)
    } catch (e: Exception) {
        return null
    }
}

expect fun modelEvents(
    url: String,
    dispatcher: CoroutineDispatcher
): Flow<ChangeListItem>


internal const val ModelEventsNamespace = "/model-events"
internal const val JoinModelEvents = "join-model-events"
internal const val ModelChangedEvent = "modelChanged"
