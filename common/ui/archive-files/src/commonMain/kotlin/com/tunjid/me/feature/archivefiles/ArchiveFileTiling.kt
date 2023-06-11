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

package com.tunjid.me.feature.archivefiles

import com.tunjid.me.core.model.ArchiveFile
import com.tunjid.me.core.model.ArchiveFileQuery
import com.tunjid.me.data.repository.ArchiveFileRepository
import com.tunjid.tiler.ListTiler
import com.tunjid.tiler.PivotRequest
import com.tunjid.tiler.Tile
import com.tunjid.tiler.listTiler

internal fun ArchiveFileRepository.archiveFilesTiler(
    limiter: Tile.Limiter<ArchiveFileQuery, ArchiveFile>
): ListTiler<ArchiveFileQuery, ArchiveFile> =
    listTiler(
        limiter = limiter,
        order = Tile.Order.Sorted(
            comparator = archiveFileQueryComparator,
        ),
        fetcher = ::files
    )

internal fun pivotRequest(gridSize: Int) = PivotRequest<ArchiveFileQuery, ArchiveFile>(
    onCount = 3 * gridSize,
    offCount = 1 * gridSize,
    nextQuery = nextArchiveFileQuery,
    previousQuery = previousArchiveFileQuery,
    comparator = archiveFileQueryComparator,
)

private val archiveFileQueryComparator = compareBy(ArchiveFileQuery::offset)

private val nextArchiveFileQuery: ArchiveFileQuery.() -> ArchiveFileQuery? = {
    copy(offset = offset + limit)
}

private val previousArchiveFileQuery: ArchiveFileQuery.() -> ArchiveFileQuery? = {
    if (offset == 0) null
    else copy(
        offset = maxOf(
            a = 0,
            b = offset - limit
        )
    )
}
