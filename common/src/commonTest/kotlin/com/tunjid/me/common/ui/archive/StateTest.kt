import com.tunjid.me.common.data.ByteSerializable
import com.tunjid.me.common.data.DelegatingByteSerializer
import com.tunjid.me.common.data.model.ArchiveQuery
import com.tunjid.me.common.data.model.ArchiveKind.Articles
import com.tunjid.me.common.data.fromBytes
import com.tunjid.me.common.data.toBytes
import com.tunjid.me.common.nav.ByteSerializableRoute
import com.tunjid.me.common.ui.archivelist.ArchiveListRoute
import com.tunjid.me.common.ui.archivelist.QueryState
import com.tunjid.me.common.ui.archivelist.State
import com.tunjid.me.common.ui.archivedetail.ArchiveDetailRoute
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.Test
import kotlin.test.assertEquals

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


class StateTest {
    @Test
    fun testStateSerialization() {

        val byteSerializer = DelegatingByteSerializer(
            format = Cbor {
                serializersModule = SerializersModule {
                    polymorphic(ByteSerializableRoute::class) {
                        subclass(ArchiveListRoute::class)
                        subclass(ArchiveDetailRoute::class)
                    }
                    polymorphic(ByteSerializable::class) {
                        subclass(State::class)
                        subclass(com.tunjid.me.common.ui.archivedetail.State::class)
                    }
                }
            }
        )

        val state = State(
            queryState = QueryState(
                startQuery = ArchiveQuery(kind = Articles),
                currentQuery = ArchiveQuery(kind = Articles)
            )
        )

        val bytes = byteSerializer.toBytes(state)

        val back = byteSerializer.fromBytes<State>(bytes)

        assertEquals(
            state,
            back
        )
    }
}

