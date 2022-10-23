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

package com.tunjid.me.scaffold.savedstate


import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.utilities.ByteSerializer
import com.tunjid.me.core.utilities.fromBytes
import com.tunjid.me.core.utilities.toBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path

@Serializable
data class SavedState(
    val isEmpty: Boolean,
    val activeNav: Int = 0,
    val navigation: List<List<String>>,
    val routeStates: Map<String, ByteArray>
) : ByteSerializable

private val defaultSavedState = SavedState(
    isEmpty = true,
    navigation = ArchiveKind.values()
        .map { "archives/${it.type}" }
        .plus("settings")
        .map(::listOf),
    routeStates = emptyMap()
)

interface SavedStateRepository {
    val savedState: StateFlow<SavedState>
    suspend fun saveState(savedState: SavedState)
}

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
        initialValue = defaultSavedState
    )

    override suspend fun saveState(savedState: SavedState) {
        dataStore.updateData { savedState }
    }
}

private class SavedStateOkioSerializer(
    private val byteSerializer: ByteSerializer
) : OkioSerializer<SavedState> {
    override val defaultValue: SavedState = defaultSavedState.copy(isEmpty = false)

    override suspend fun readFrom(source: BufferedSource): SavedState =
        byteSerializer.fromBytes(source.readByteArray())

    override suspend fun writeTo(savedState: SavedState, sink: BufferedSink) {
        sink.write(byteSerializer.toBytes(savedState))
    }
}