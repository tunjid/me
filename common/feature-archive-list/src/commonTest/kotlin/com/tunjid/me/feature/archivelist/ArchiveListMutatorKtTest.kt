import app.cash.turbine.test
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.me.core.model.ArchiveQuery
import com.tunjid.me.feature.archivelist.Action
import com.tunjid.me.feature.archivelist.QueryState
import com.tunjid.me.feature.archivelist.State
import com.tunjid.me.feature.archivelist.filterToggleMutations
import com.tunjid.mutator.coroutines.reduceInto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
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


class ArchiveListMutatorKtTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testFilterToggleMutations() = runTest {
        val initialQueryState = QueryState(
            expanded = false,
            startQuery = ArchiveQuery(kind = ArchiveKind.Articles),
            currentQuery = ArchiveQuery(kind = ArchiveKind.Articles),
        )
        val initialState = State(
            queryState = initialQueryState
        )
        listOf(
            Action.ToggleFilter(isExpanded = null),
            Action.ToggleFilter(isExpanded = null),
            Action.ToggleFilter(isExpanded = true),
            Action.ToggleFilter(isExpanded = true),
            Action.ToggleFilter(isExpanded = true),
            Action.ToggleFilter(isExpanded = true),
            Action.ToggleFilter(isExpanded = false),
        )
            .asFlow()
            .filterToggleMutations()
            .reduceInto(initialState)
            .test {
                // First item is initial state
                assertEquals(
                    expected = initialState,
                    actual = awaitItem()
                )
                // After processing null for `isExpanded`, the toggle should have flipped
                assertEquals(
                    expected = initialState.queryState.copy(expanded = true),
                    actual = awaitItem().queryState
                )
                // Toggle toggle expanded
                assertEquals(
                    expected = initialState.queryState.copy(expanded = false),
                    actual = awaitItem().queryState
                )
                // Explicit set expanded to true
                assertEquals(
                    expected = initialState.queryState.copy(expanded = true),
                    actual = awaitItem().queryState
                )

                // Consecutive emissions of true should have been ignored

                // Explicit set expanded to false
                assertEquals(
                    expected = initialState.queryState.copy(expanded = false),
                    actual = awaitItem().queryState
                )

                awaitComplete()
            }
    }
}