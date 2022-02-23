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

import com.tunjid.serverevents.ServerEvent
import com.tunjid.serverevents.serverEvents
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

actual fun modelEvents(
    url: String,
    dispatcher: CoroutineDispatcher
): Flow<ModelEvent> = serverEvents(
    url = url,
    namespace = ModelEventsNamespace,
    events = listOf(
        ModelChangedEvent,
        ModelDeletedEvent
    ),
    dispatcher = dispatcher,
    onResponse = { response ->
        if (response is ServerEvent.Response.Manager && response.event == "open")
            ServerEvent.Request(
                event = JoinModelEvents,
                arguments = listOf("hello")
            )
        else null
    }
)
    .filterIsInstance<ServerEvent.Response.Socket>()
    .map { it.arguments.toArchiveEvent(it.event) }
    .filterNotNull()
