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

package com.tunjid.me.core.utilities

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

@Composable
fun <T, R> StateFlow<T>.mappedCollectAsState(
    context: CoroutineContext = kotlin.coroutines.EmptyCoroutineContext,
    mapper: (T) -> R
): State<R> {
    val scope = rememberCoroutineScope()
    return mapState(scope = scope, mapper = mapper).collectAsState(context = context)
}

infix fun Dp.countIf(condition: Boolean) = if (condition) this else 0.dp

private fun <T, R> StateFlow<T>.mapState(
    scope: CoroutineScope,
    mapper: (T) -> R
): StateFlow<R> = map { mapper(it) }
    .distinctUntilChanged()
    .stateIn(
        scope = scope,
        initialValue = mapper(value),
        started = SharingStarted.WhileSubscribed(2000),
    )
