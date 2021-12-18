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

package com.tunjid.me.ui.archivedetail

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.RichText
import com.tunjid.me.LocalAppDependencies
import com.tunjid.me.data.archive.Archive
import com.tunjid.me.nav.Route

data class ArchiveDetailRoute(val archive: Archive) : Route<ArchiveDetailMutator> {
    override val id: String
        get() = archive.key

    @Composable
    @ExperimentalMaterialApi
    override fun Render() {
        ArchiveDetailScreen(
            mutator = LocalAppDependencies.current.routeDependencies(this)
        )
    }
}

@Composable
private fun ArchiveDetailScreen(mutator: ArchiveDetailMutator) {
    val state by mutator.state.collectAsState()
    val scrollState = rememberScrollState()

    RichText(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(state = scrollState)
    ) {
        Markdown(
            content = state.archive.body
        )
    }
}
