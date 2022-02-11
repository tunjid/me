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

package com.tunjid.me.common.data.repository

import com.tunjid.me.common.data.local.SessionCookieDao
import com.tunjid.me.common.data.model.Result
import com.tunjid.me.common.data.model.SessionRequest
import com.tunjid.me.common.data.model.User
import com.tunjid.me.common.data.network.NetworkService
import com.tunjid.me.common.data.network.exponentialBackoff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface AuthRepository {
    val isSignedIn: Flow<Boolean>
    val signedInUserStream: Flow<User?>
    suspend fun createSession(request: SessionRequest): Result
}

class SessionCookieAuthRepository(
    private val networkService: NetworkService,
    dao: SessionCookieDao
) : AuthRepository {

    override val isSignedIn: Flow<Boolean> =
        dao.sessionCookieStream.map { it != null }
            .distinctUntilChanged()

    override val signedInUserStream: Flow<User?> =
        isSignedIn.map {
            if (it) exponentialBackoff(
                default = null,
                block = { networkService.session() }
            )
            else null
        }

    override suspend fun createSession(request: SessionRequest): Result = try {
        networkService.signIn(request)
        Result.Success
    } catch (e: Throwable) {
        Result.Error(e.message)
    }
}