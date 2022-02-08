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

package com.tunjid.me.common.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

sealed class ModelEvent {
    abstract val collection: String
    abstract val id: String

    data class Changed(
        override val collection: String,
        override val id: String,
    ) : ModelEvent()

    data class Deleted(
        override val collection: String,
        override val id: String,
    ) : ModelEvent()
}

fun List<Any>.toArchiveEvent(event: String): ModelEvent? {
    if (size < 2) return null
    val (collection, id) = this
    return if (collection is String && id is String) when (event) {
        "modelChanged" -> ModelEvent.Changed(collection, id)
        "modelDeleted" -> ModelEvent.Deleted(collection, id)
        else -> null
    }
    else null
}

expect fun modelEvents(
    url: String,
    dispatcher: CoroutineDispatcher
): Flow<ModelEvent>



internal const val ModelEventsNamespace = "/model-events"
internal const val JoinModelEvents = "join-model-events"
internal const val ModelChangedEvent = "modelChanged"
internal const val ModelDeletedEvent = "modelDeleted"
