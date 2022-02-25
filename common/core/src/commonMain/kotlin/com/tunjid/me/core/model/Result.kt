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

sealed class Result<T> {
    data class Success<T>(val item: T) : Result<T>()
    data class Error<T>(val message: String?) : Result<T>()
}

fun <T, R> Result<T>.map(mapper: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(mapper(item))
    is Result.Error -> Result.Error(message = message)
}