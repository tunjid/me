/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.me.scaffold.scaffold

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.me.core.model.User
import com.tunjid.me.core.ui.AsyncRasterImage
import com.tunjid.me.core.ui.MediaArgs
import com.tunjid.treenav.compose.threepane.ThreePane
import me.common.scaffold.generated.resources.Res
import me.common.scaffold.generated.resources.go_back
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PaneScaffoldState.RootDestinationTopAppBar(
    modifier: Modifier = Modifier,
    signedInProfile: User?,
    title: @Composable () -> Unit = {},
    onSignedInProfileClicked: (User, String) -> Unit,
) {
    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(36.dp)
            )
        },
        title = title,
        actions = {
            AnimatedVisibility(
                visible = signedInProfile != null
            ) {
                signedInProfile?.let { profile ->
                    AsyncRasterImage(
                        modifier = Modifier
                            .size(36.dp)
                            .sharedElement(
                                key = SignedInUserAvatarSharedElementKey,
                            )
                            .clickable {
                                onSignedInProfileClicked(
                                    profile,
                                    SignedInUserAvatarSharedElementKey
                                )
                            },
                        args = remember(profile) {
                            MediaArgs(
                                url = profile.imageUrl,
                                description = signedInProfile.fullName,
                                contentScale = ContentScale.Crop,
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
        },
    )
}

@Composable
fun PaneScaffoldState.PoppableDestinationTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    onBackPressed: () -> Unit,
) {
    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        ),
        navigationIcon = {
            AnimatedVisibility(
                visible = paneState.pane == ThreePane.Primary,
                enter = fadeIn(),
                exit = fadeOut(),
                content = {
                    FilledTonalIconButton(
                        modifier = Modifier,
                        onClick = onBackPressed,
                    ) {
                        Icon(
                            imageVector =
                                if (paneState.pane == ThreePane.Primary) Icons.AutoMirrored.Rounded.ArrowBack
                                else Icons.Rounded.Close,
                            contentDescription = stringResource(Res.string.go_back),
                        )
                    }
                }
            )
        },
        title = title,
        actions = actions,
    )
}

private const val SignedInUserAvatarSharedElementKey = "self"
