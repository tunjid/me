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

package com.tunjid.me.core.model

import com.tunjid.me.core.utilities.uuid


/**
 * Message queue for notifying the UI according to the UI events guidance in the
 * [Android architecture docs](https://developer.android.com/jetpack/guide/ui-layer/events#handle-viewmodel-events)
 *
 */
data class MessageQueue(
    val items: List<Message> = listOf()
)

data class Message(
    val value: String,
    val id: String = uuid(),
)

fun MessageQueue.peek(): Message? = items.firstOrNull()

operator fun MessageQueue.plus(message: Message): MessageQueue = copy(
    items = items + message
)

operator fun MessageQueue.plus(message: String): MessageQueue = copy(
    items = items + Message(value = message)
)

operator fun MessageQueue.minus(message: Message): MessageQueue = copy(
    items = items - message
)