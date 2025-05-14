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


import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import com.tunjid.me.core.model.UserId
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.core.utilities.fromBytes
import com.tunjid.me.core.utilities.toBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path

@Serializable
data class SavedState(
    val auth: AuthTokens?,
    val navigation: Navigation,
) : ByteSerializable {

    @Serializable
    data class AuthTokens(
        val authUserId: UserId?,
        val token: String?,
    )

    @Serializable
    data class Navigation(
        val activeNav: Int = 0,
        val backStacks: List<List<String>> = emptyList(),
    )
}

val InitialSavedState = SavedState(
    auth = null,
    navigation = SavedState.Navigation(activeNav = -1),
)

val EmptySavedState = SavedState(
    auth = null,
    navigation = SavedState.Navigation(activeNav = 0),
)

interface SavedStateRepository {
    val savedState: StateFlow<SavedState>
    suspend fun updateState(update: SavedState.() -> SavedState)
}

@Inject
class DataStoreSavedStateRepository(
    path: Path,
    appScope: CoroutineScope,
    byteSerializer: ByteSerializer
) : SavedStateRepository {

    private val dataStore: DataStore<SavedState> = DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = SavedStateOkioSerializer(byteSerializer),
            producePath = { path }
        ),
        scope = appScope
    )

    override val savedState = dataStore.data.stateIn(
        scope = appScope,
        started = SharingStarted.Eagerly,
        initialValue = InitialSavedState,
    )

    override suspend fun updateState(update: SavedState.() -> SavedState) {
        dataStore.updateData(update)
    }
}

private class SavedStateOkioSerializer(
    private val byteSerializer: ByteSerializer
) : OkioSerializer<SavedState> {
    override val defaultValue: SavedState = EmptySavedState

    override suspend fun readFrom(source: BufferedSource): SavedState =
        byteSerializer.fromBytes(source.readByteArray())

    override suspend fun writeTo(savedState: SavedState, sink: BufferedSink) {
        sink.write(byteSerializer.toBytes(savedState))
    }
}