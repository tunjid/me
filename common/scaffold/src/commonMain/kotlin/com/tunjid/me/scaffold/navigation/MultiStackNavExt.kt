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

package com.tunjid.me.scaffold.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.tunjid.me.core.model.ArchiveKind
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.canPop
import com.tunjid.treenav.popToRoot
import com.tunjid.treenav.switch

val MultiStackNav.canGoUp get() = stacks.getOrNull(currentIndex)?.canPop == true

val MultiStackNav.navItems
    get() = stacks
        .map(StackNav::name)
        .mapIndexed { index, name ->
            val kind = ArchiveKind.entries.firstOrNull {
                name.contains(it.type)
            }
            NavItem(
                name = kind?.type ?: name,
                icon = kind?.icon ?: Icons.Default.Settings,
                index = index,
                selected = currentIndex == index,
            )
        }

fun MultiStackNav.navItemSelected(item: NavItem) =
    if (item.selected) popToRoot(indexToPop = item.index)
    else switch(toIndex = item.index)
