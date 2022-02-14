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

package com.tunjid.me.common.ui.archivelist

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.me.core.model.Archive
import com.tunjid.me.core.model.ArchiveId
import com.tunjid.me.core.model.ArchiveKind.Articles
import com.tunjid.me.core.model.ArchiveQuery
import com.tunjid.me.core.model.Descriptor
import com.tunjid.me.core.model.User
import com.tunjid.me.core.model.UserId
import com.tunjid.me.core.model.plus
import com.tunjid.me.nav.AppRoute
import com.tunjid.me.common.ui.archivedetail.ArchiveDetailRoute
import com.tunjid.me.common.ui.common.Chips
import com.tunjid.me.common.ui.common.RemoteImagePainter
import kotlinx.datetime.Clock.System

@Composable
fun ProgressBar(isCircular: Boolean) {
    Box(
        modifier = Modifier
            .padding(vertical = 24.dp)
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        if (isCircular) CircularProgressIndicator(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.Center),
            color = MaterialTheme.colors.onSurface
        )
        else LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .align(Alignment.Center),
            color = MaterialTheme.colors.onSurface
        )
    }
}

@Composable
fun ArchiveCard(
    archiveItem: ArchiveItem.Result,
    onRouteSelected: (AppRoute<*>) -> Unit,
    onAction: (Action) -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(16.dp),
        onClick = {
            onRouteSelected(
                ArchiveDetailRoute(
                    kind = archiveItem.archive.kind,
                    archiveId = archiveItem.archive.id
                )
            )
        },
        content = {
            Column {
                ArchiveThumbnail(archiveItem.archive.thumbnail)
                Spacer(Modifier.height(8.dp))
                ArchiveCategories(
                    categories = archiveItem.archive.categories,
                    published = archiveItem.prettyDate,
                    onCategoryClicked = { category ->
                        val query = archiveItem.query
                        onAction(
                            Action.Fetch.Reset(
                                query = query.copy(offset = 0) + category
                            )
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
                ArchiveBlurb(archiveItem = archiveItem)
                Spacer(Modifier.height(24.dp))
                ArchiveCardFooter(
                    authorImageUrl = archiveItem.archive.author.imageUrl,
                    authorName = archiveItem.archive.author.fullName,
                    timeToRead = archiveItem.readTime,
                    likeCount = archiveItem.archive.likes
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    )
}

@Composable
private fun ArchiveThumbnail(imageUrl: String?) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
    val painter = RemoteImagePainter(imageUrl)

    if (painter != null) Image(
        painter = painter,
        contentScale = ContentScale.Crop,
        contentDescription = null,
        modifier = modifier
    )
    else Box(modifier = modifier)
}

@Composable
private fun ArchiveCategories(
    categories: List<com.tunjid.me.core.model.Descriptor.Category>,
    published: String,
    onCategoryClicked: (com.tunjid.me.core.model.Descriptor.Category) -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Chips(
            modifier = Modifier.weight(1F),
            chips = categories.map(com.tunjid.me.core.model.Descriptor.Category::value),
            color = MaterialTheme.colors.primaryVariant,
            onClick = { onCategoryClicked(com.tunjid.me.core.model.Descriptor.Category(it)) }
        )
        Text(
            modifier = Modifier.wrapContentWidth(),
            text = published,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ArchiveBlurb(archiveItem: ArchiveItem.Result) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = archiveItem.archive.title,
            fontSize = 18.sp,
        )
        Spacer(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = archiveItem.archive.description,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun ArchiveCardFooter(
    authorImageUrl: String?,
    authorName: String,
    timeToRead: String,
    likeCount: Long,
) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val height = 40.dp
        val modifier = Modifier
            .size(height)
        val painter = RemoteImagePainter(authorImageUrl)

        if (painter != null) {
            Image(
                painter = painter,
                contentScale = ContentScale.Crop,
                contentDescription = null,
                modifier = modifier.clip(CircleShape)
            )
            Spacer(Modifier.width(8.dp))
        } else Box(modifier = modifier)
        Column(
            modifier = Modifier.height(height),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.wrapContentWidth(),
                text = authorName,
                fontSize = 16.sp,
            )
            Text(
                modifier = Modifier.wrapContentWidth(),
                text = "$likeCount ❤️ • $timeToRead",
                fontSize = 12.sp,
            )
        }
    }
}

private val sampleArchiveItem = ArchiveItem.Result(
    query = com.tunjid.me.core.model.ArchiveQuery(kind = Articles),
    archive = com.tunjid.me.core.model.Archive(
        id = com.tunjid.me.core.model.ArchiveId(""),
        link = "https://storage.googleapis.com/tunji-web-public/article-media/1P372On2TSH-rAuBsbWLGSQ.jpeg",
        title = "I'm an Archive",
        body = "Hello",
        description = "Hi",
        thumbnail = "https://storage.googleapis.com/tunji-web-public/article-media/1P372On2TSH-rAuBsbWLGSQ.jpeg",
        author = com.tunjid.me.core.model.User(
            id = com.tunjid.me.core.model.UserId("i"),
            firstName = "TJ",
            lastName = "D",
            fullName = "TJ D",
            imageUrl = "",
        ),
        created = System.now(),
        tags = listOf(),
        categories = listOf("Android", "Kotlin").map(com.tunjid.me.core.model.Descriptor::Category),
        kind = Articles,
        likes = 1L
    )
)
