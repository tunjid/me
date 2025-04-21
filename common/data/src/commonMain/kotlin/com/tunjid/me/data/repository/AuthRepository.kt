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

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.tunjid.me.common.data.UserEntityQueries
import com.tunjid.me.core.model.Result
import com.tunjid.me.core.model.SessionRequest
import com.tunjid.me.core.model.User
import com.tunjid.me.core.model.UserId
import com.tunjid.me.core.model.map
import com.tunjid.me.data.local.models.toExternalModel
import com.tunjid.me.data.network.NetworkService
import com.tunjid.me.data.network.models.toResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

interface AuthRepository {
    val isSignedIn: Flow<Boolean>

    val signedInUserStream: Flow<User?>
    suspend fun createSession(request: SessionRequest): Result<UserId>
}

@Inject
internal class SessionCookieAuthRepository(
    private val networkService: NetworkService,
    private val userEntityQueries: UserEntityQueries,
    private val savedStateRepository: SavedStateRepository,
    dispatcher: CoroutineDispatcher,
) : AuthRepository {

    override val signedInUserStream: Flow<User?> =
        savedStateRepository.savedState
            .distinctUntilChangedBy { it.auth?.authUserId }
            .flatMapLatest { savedState ->
                savedState.auth
                    ?.authUserId
                    ?.value
                    ?.let(userEntityQueries::find)
                    ?.asFlow()
                    ?.mapToOneOrNull(context = dispatcher)
                    ?.map { it?.toExternalModel() }
                    ?: flowOf(null)
            }

    override val isSignedIn: Flow<Boolean> =
        savedStateRepository.savedState.map { it.auth != null }

    override suspend fun createSession(request: SessionRequest): Result<UserId> =
        networkService.signIn(request)
            .toResult()
            .also { authTokensResult ->
                if (authTokensResult !is Result.Success) return@also
                savedStateRepository.updateState {
                    copy(auth = authTokensResult.item)
                }
                // Suspend till auth token has been saved and is readable
                savedStateRepository.savedState.first { it.auth != null }
            }
            .map(SavedState.AuthTokens::authUserId)
}