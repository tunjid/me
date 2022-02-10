import com.tunjid.me.common.SavedState
import com.tunjid.me.common.data.ByteSerializable
import com.tunjid.me.common.data.DelegatingByteSerializer
import com.tunjid.me.common.data.model.ArchiveQuery
import com.tunjid.me.common.data.model.ArchiveKind.Articles
import com.tunjid.me.common.data.fromBytes
import com.tunjid.me.common.data.toBytes
import com.tunjid.me.common.nav.ByteSerializableRoute
import com.tunjid.me.common.nav.toByteSerializable
import com.tunjid.me.common.ui.archive.ArchiveRoute
import com.tunjid.me.common.ui.archive.QueryState
import com.tunjid.me.common.ui.archive.State
import com.tunjid.me.common.ui.archivedetail.ArchiveDetailRoute
import com.tunjid.treenav.MultiStackNav
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


class StateRestorationTest {
    @Test
    fun testStateSerialization() {
        val byteSerializer = DelegatingByteSerializer(
            format = Cbor {
                serializersModule = SerializersModule {
                    polymorphic(ByteSerializableRoute::class) {
                        subclass(ArchiveRoute::class)
                        subclass(ArchiveDetailRoute::class)
                    }
                    polymorphic(ByteSerializable::class) {
                        subclass(State::class)
                        subclass(com.tunjid.me.common.ui.archivedetail.State::class)
                    }
                }
            }
        )
        val nav = MultiStackNav(
            name = ""
        )
        val state = State(
            queryState = QueryState(
                startQuery = ArchiveQuery(kind = Articles),
                currentQuery = ArchiveQuery(kind = Articles)
            )
        )
        val savedState = SavedState(
            navigation = byteSerializer.toBytes(nav.toByteSerializable),
            routeStates = mapOf(
                ArchiveRoute(
                    state.queryState.currentQuery
                ).id to byteSerializer.toBytes(state)
            )
        )


        val bytes = byteSerializer.toBytes(savedState)

        val back = byteSerializer.fromBytes<SavedState>(bytes)

        assertEquals(
            savedState,
            back
        )
    }
}

