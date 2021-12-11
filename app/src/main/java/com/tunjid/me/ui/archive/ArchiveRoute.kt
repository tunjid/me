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
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.tunjid.me.data.archive.Archive
import com.tunjid.me.data.archive.ArchiveQuery
import com.tunjid.me.nav.Route

data class ArchiveRoute(val query: ArchiveQuery) : Route {
    @Composable
    override fun Render() {

    }
}

@ExperimentalMaterialApi
@Composable
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