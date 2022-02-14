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

package com.tunjid.me.data.repository

import com.tunjid.me.data.local.SessionCookieDao
import com.tunjid.me.data.network.NetworkService
import com.tunjid.me.data.network.exponentialBackoff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface AuthRepository {
    val isSignedIn: Flow<Boolean>
    val signedInUserStream: Flow<com.tunjid.me.core.model.User?>
    suspend fun createSession(request: com.tunjid.me.core.model.SessionRequest): com.tunjid.me.core.model.Result<com.tunjid.me.core.model.UserId>
}

internal class SessionCookieAuthRepository(
    private val networkService: NetworkService,
    dao: SessionCookieDao
) : AuthRepository {

    override val isSignedIn: Flow<Boolean> =
        dao.sessionCookieStream.map { it != null }
            .distinctUntilChanged()

    override val signedInUserStream: Flow<com.tunjid.me.core.model.User?> =
        isSignedIn.map {
            if (it) exponentialBackoff(
                default = null,
                block = { networkService.session() }
            )
            else null
        }

    override suspend fun createSession(request: com.tunjid.me.core.model.SessionRequest): com.tunjid.me.core.model.Result<com.tunjid.me.core.model.UserId> = try {
        val user = networkService.signIn(request)
        com.tunjid.me.core.model.Result.Success(user.id)
    } catch (e: Throwable) {
        com.tunjid.me.core.model.Result.Error(e.message)
    }
}