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

package com.tunjid.me.feature.archivelist

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.tunjid.me.core.ui.AsyncRasterImage
import com.tunjid.me.core.ui.ChipInfo
import com.tunjid.me.core.ui.Chips
import com.tunjid.me.core.ui.rememberAsyncRasterPainter
import kotlinx.datetime.Clock.System

@Composable
fun StickyHeader(
    modifier: Modifier = Modifier,
    item: ArchiveItem.Header
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(0.dp),
        tonalElevation = 0.dp
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 16.dp,
                    horizontal = 8.dp
                ),
            text = item.text
        )
    }
}

@Composable
fun ProgressBar(
    modifier: Modifier = Modifier,
    isCircular: Boolean
) {
    Box(
        modifier = modifier
            .padding(vertical = 24.dp)
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        if (isCircular) CircularProgressIndicator(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.Center),
            color = MaterialTheme.colorScheme.onSurface
        )
        else LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .align(Alignment.Center),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ArchiveCard(
    modifier: Modifier = Modifier,
    query: ArchiveQuery,
    archive: Archive,
    onArchiveSelected: (Archive) -> Unit,
    onCategoryClicked: (Descriptor.Category) -> Unit,
) {
    ElevatedCard(
        modifier = modifier,
        onClick = {
            onArchiveSelected(archive)
        },
        content = {
            Column {
                AsyncRasterImage(
                    imageUrl = archive.thumbnail,
                    modifier = modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )
                Spacer(Modifier.height(8.dp))
                ArchiveDate(archive.prettyDate)
                Spacer(Modifier.height(8.dp))
                ArchiveDetails(
                    title = archive.title,
                    description = archive.description,
                    chipInfoList = archive.descriptorChips<Descriptor.Category>(query = query),
                    onCategoryClicked = onCategoryClicked
                )
                Spacer(Modifier.height(24.dp))
                ArchiveCardFooter(
                    authorImageUrl = archive.author.imageUrl,
                    authorName = archive.author.fullName,
                    timeToRead = archive.readTime,
                    likeCount = archive.likes
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    )
}

@Composable
private fun ArchiveDetails(
    title: String,
    description: String,
    chipInfoList: List<ChipInfo>,
    onCategoryClicked: (Descriptor.Category) -> Unit
) {
    ArchiveBlurb(
        title = title,
        description = description,
    )
    Spacer(Modifier.height(4.dp))
    ArchiveCategories(
        chipInfoList = chipInfoList,
        onCategoryClicked = onCategoryClicked
    )
}

@Composable
private fun ArchiveDate(prettyDate: String) {
    Text(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .wrapContentWidth(),
        text = prettyDate,
        fontSize = 12.sp,
    )
}

@Composable
private fun ArchiveCategories(
    chipInfoList: List<ChipInfo>,
    onCategoryClicked: (Descriptor.Category) -> Unit
) {
    Chips(
        modifier = Modifier.padding(horizontal = 8.dp),
        chipInfoList = chipInfoList,
        onClick = { onCategoryClicked(Descriptor.Category(it.text)) }
    )
}

@Composable
private fun ArchiveBlurb(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
        )
        Spacer(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = description,
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
        val painter = rememberAsyncRasterPainter(authorImageUrl)

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

private val sampleArchiveItem = ArchiveItem.Card.Loaded(
    index = 0,
    key = "",
    archive = Archive(
        id = ArchiveId(""),
        link = "https://storage.googleapis.com/tunji-web-public/article-media/1P372On2TSH-rAuBsbWLGSQ.jpeg",
        title = "I'm an Archive",
        body = "Hello",
        description = "Hi",
        thumbnail = "https://storage.googleapis.com/tunji-web-public/article-media/1P372On2TSH-rAuBsbWLGSQ.jpeg",
        videoUrl = null,
        author = User(
            id = UserId("i"),
            firstName = "TJ",
            lastName = "D",
            fullName = "TJ D",
            imageUrl = "",
        ),
        created = System.now(),
        tags = listOf(),
        categories = listOf("Android", "Kotlin").map(Descriptor::Category),
        kind = Articles,
        likes = 1L
    )
)
