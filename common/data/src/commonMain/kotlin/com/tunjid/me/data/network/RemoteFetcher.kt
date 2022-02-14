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

import com.tunjid.tiler.Tile
import com.tunjid.tiler.tiledList
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

/**
 * Fetches [Item] from [fetch] using [Query] and saves it with [save]
 */
fun <Query, Item> remoteFetcher(
    scope: CoroutineScope,
    networkMonitor: NetworkMonitor,
    fetch: suspend (Query) -> Item,
    save: suspend (Item) -> Unit
): suspend (Tile.Request<Query, Item>) -> Unit = BackOffFetcher(
    scope = scope,
    networkMonitor = networkMonitor,
    fetch = fetch,
    save = save
)::load

private class BackOffFetcher<Query, Item>(
    scope: CoroutineScope,
    networkMonitor: NetworkMonitor,
    fetch: suspend (Query) -> Item,
    save: suspend (Item) -> Unit
) {

    val requests = MutableSharedFlow<Tile.Request<Query, Item>>()

    val queryTiler: (Flow<Tile.Input.List<Query, Item>>) -> Flow<List<Item>> =
        tiledList { query ->
            networkMonitor
                .isConnected
                .filter { it }
                .distinctUntilChanged()
                .mapNotNull {
                    exponentialBackoff(
                        initialDelay = 1_000,
                        maxDelay = 20_000,
                        default = null,
                    ) { fetch(query) }
                }
                .take(1)
                .onEach(save)
        }

    init {
        queryTiler(requests).launchIn(scope)
    }

    suspend fun load(request: Tile.Request<Query, Item>) = requests.emit(request)
}

suspend fun <T> exponentialBackoff(
    times: Int = Int.MAX_VALUE,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    default: T,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(times) {
        try {
            return block()
        } catch (e: IOException) {
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return default
}

