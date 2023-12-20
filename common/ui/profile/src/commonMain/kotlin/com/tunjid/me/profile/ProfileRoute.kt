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

package com.tunjid.me.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.me.core.ui.AsyncRasterImage
import com.tunjid.me.core.ui.FormField
import com.tunjid.me.feature.rememberRetainedStateHolder
import com.tunjid.me.scaffold.adaptive.AdaptiveRoute
import com.tunjid.me.scaffold.globalui.InsetFlags
import com.tunjid.me.scaffold.globalui.NavVisibility
import com.tunjid.me.scaffold.globalui.ScreenUiState
import com.tunjid.me.scaffold.globalui.UiState
import com.tunjid.me.scaffold.lifecycle.collectAsStateWithLifecycle
import com.tunjid.me.scaffold.navigation.SerializedRouteParams
import com.tunjid.me.scaffold.scaffold.backPreviewBackgroundModifier
import kotlinx.serialization.Serializable

@Serializable
data class ProfileRoute(
    override val routeParams: SerializedRouteParams,
) : AdaptiveRoute {
    @Composable
    override fun content() {
        val stateHolder = rememberRetainedStateHolder<ProfileStateHolder>(
            route = this@ProfileRoute
        )
        ProfileScreen(
            state = stateHolder.state.collectAsStateWithLifecycle().value,
            actions = stateHolder.accept,
            modifier = Modifier.backPreviewBackgroundModifier(),
        )
    }
}

@Composable
private fun ProfileScreen(
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    ScreenUiState(
        UiState(
            toolbarShows = true,
            toolbarTitle = "Profile",
            navVisibility = NavVisibility.Gone,
            insetFlags = InsetFlags.NO_BOTTOM,
            statusBarColor = MaterialTheme.colorScheme.surface.toArgb(),
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(state = scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val user = state.signedInUser

        Spacer(modifier = Modifier.height(32.dp))
        val imageModifier = Modifier
            .padding(horizontal = 16.dp)
            .size(96.dp)

        if (user != null) AsyncRasterImage(
            imageUrl = user.imageUrl,
            modifier = modifier.clip(CircleShape),
        ) else Image(
            imageVector = Icons.Default.Person,
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = imageModifier.clip(CircleShape)
        )

        state.fields.forEach { field ->
            Spacer(modifier = Modifier.height(8.dp))
            FormField(
                modifier = Modifier
                    .fillMaxWidth(0.6f),
                field = field,
                onValueChange = {
                    actions(Action.FieldChanged(field = field.copy(value = it)))
                }
            )
        }
    }
}
