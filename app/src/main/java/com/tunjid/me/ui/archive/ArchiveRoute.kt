/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.me.ui.archive

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.sp
import com.tunjid.me.LocalAppDependencies
import com.tunjid.me.data.archive.Archive
import com.tunjid.me.data.archive.ArchiveQuery
import com.tunjid.me.globalui.UiState
import com.tunjid.me.nav.Route
import com.tunjid.me.ui.InitialUiState

data class ArchiveRoute(val query: ArchiveQuery) : Route<ArchiveMutator> {
    override val id: String
        get() = query.toString()

    @Composable
    @ExperimentalMaterialApi
    override fun Render() {

        InitialUiState(
            UiState(
                toolbarTitle = query.kind.type,
                showsBottomNav = true
            )
        )

        val mutator = LocalAppDependencies.current.routeDependencies(this)
        val state by mutator.state.collectAsState()

        val listState = rememberLazyListState()
        LazyColumn(state = listState) {
            items(
                items = state.archives,
                key = Archive::key,
                itemContent = { ArchiveCard(it) }
            )
        }

        LaunchedEffect(true) {
            mutator.accept(Action.Fetch(query = query))
        }
    }
}

@Composable
@ExperimentalMaterialApi
private fun ArchiveCard(archive: Archive) {
    Card(
        onClick = { /*TODO*/ },
        content = {
            Column {
                ArchiveBlurb(archive = archive)
            }
        }
    )
}

@Composable
private fun ArchiveBlurb(archive: Archive) {
    Column {
        Text(
            text = archive.title,
            fontSize = 17.sp,
        )
        Text(
            text = archive.description,
            fontSize = 14.sp,
        )
    }
}