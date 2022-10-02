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

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import com.tunjid.me.common.data.SessionEntityQueries
import com.tunjid.me.common.data.UserEntityQueries
import com.tunjid.me.core.model.Result
import com.tunjid.me.core.model.SessionRequest
import com.tunjid.me.core.model.User
import com.tunjid.me.core.model.UserId
import com.tunjid.me.core.model.map
import com.tunjid.me.data.local.toUser
import com.tunjid.me.data.network.NetworkService
import com.tunjid.me.data.network.models.NetworkUser
import com.tunjid.me.data.network.models.item
import com.tunjid.me.data.network.models.toResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface AuthRepository {
    val isSignedIn: Flow<Boolean>
    val signedInUserStream: Flow<User?>
    suspend fun createSession(request: SessionRequest): Result<UserId>
}

internal class SessionCookieAuthRepository(
    private val networkService: NetworkService,
    private val userEntityQueries: UserEntityQueries,
    sessionEntityQueries: SessionEntityQueries,
    dispatcher: CoroutineDispatcher,
) : AuthRepository {

    override val signedInUserStream: Flow<User?> =
        sessionEntityQueries.cookie()
            .asFlow()
            .mapToList(context = dispatcher)
            .map { networkService.session().item() }
            .flatMapLatest { networkUser ->
                if (networkUser == null) flowOf(null)
                else userEntityQueries.find(networkUser.id.value)
                    .asFlow()
                    .mapToOneOrNull(context = dispatcher)
                    .map { it?.toUser }
            }

    override val isSignedIn: Flow<Boolean> =
        signedInUserStream.map { it != null }

    override suspend fun createSession(request: SessionRequest): Result<UserId> =
        networkService.signIn(request)
            .toResult()
            .also {
                if (it is Result.Success)
                    userEntityQueries.upsert(
                        id = it.item.id.value,
                        first_name = it.item.firstName,
                        last_name = it.item.lastName,
                        full_name = it.item.fullName,
                        image_url = it.item.imageUrl,
                    )
            }
            .map(NetworkUser::id)
}