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

package com.tunjid.me.scaffold.permissions

import com.tunjid.mutator.ActionStateMutator
import kotlinx.coroutines.flow.StateFlow

interface PermissionsProvider {
    val stateHolder: PermissionsStateHolder
}

typealias PermissionsStateHolder = ActionStateMutator<Permission, StateFlow<Permissions>>

sealed class Permission {
    object ReadExternalStorage : Permission()
}

interface Permissions {
    fun isGranted(permission: Permission): Boolean
}

internal data class MapPermissions(
    val permissionsMap: Map<Permission, Boolean>
) : Permissions {
    override fun isGranted(permission: Permission): Boolean =
        permissionsMap[permission] ?: false
}

expect class PlatformPermissionsProvider : PermissionsProvider