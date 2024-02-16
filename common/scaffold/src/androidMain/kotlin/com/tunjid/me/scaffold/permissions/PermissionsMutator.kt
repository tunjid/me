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

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.tunjid.mutator.ActionStateMutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.stateIn
import lift.lastChangedActivity
import lift.onActivitiesChanged

private data class PermissionsCache(
    val activeActivity: ComponentActivity? = null,
    val map: Map<ComponentActivity, Map<String, ActionStateMutator<Unit, StateFlow<Boolean>>>> = mapOf()
)

/**
 * Creates a [StateHolder] whose state is a map of the requested [permissions] to their granted status.
 * The resulting [StateHolder] is best used as an [Application] scoped instance registered before
 * the launcher [Activity] reaches its [Lifecycle.State.CREATED] [Lifecycle.State].
 */
internal fun Context.permissionsStateHolder(
    scope: CoroutineScope,
    permissions: List<Permission>
): PermissionsStateHolder =
    permissionsCacheStateHolder(permissions.mapNotNull(Permission::toManifestString)).let { permissionsCacheStateHolder ->
        object : PermissionsStateHolder {
            override val state: StateFlow<Permissions> = permissionsCacheStateHolder.state
                .flatMapLatest { cache ->
                    // Flatten output state of each individual permission mutator.
                    val permissionsToStateHolders: Map<String, ActionStateMutator<Unit, StateFlow<Boolean>>>? =
                        cache.activeActivity?.let(cache.map::get)

                    val flows: List<Flow<Pair<String, Boolean>>>? = permissionsToStateHolders?.entries
                        ?.map { (permission, stateHolder) -> stateHolder.state.map(permission::to) }

                    if (flows != null) combine(flows) {
                        MapPermissions(permissionsMap = it.mapNotNull { (key, value) ->
                            when (val permission = key.toPermission) {
                                null -> null
                                else -> permission to value
                            }
                        }
                            .toMap())
                    }
                    else emptyFlow()
                }
                .stateIn(
                    scope = scope,
                    initialValue = MapPermissions(mapOf()),
                    started = SharingStarted.Eagerly
                )

            override val accept: (Permission) -> Unit = { permission ->
                permission.toManifestString?.let(permissionsCacheStateHolder.accept)
            }
        }
    }

/**
 * Creates a [StateHolder] which reduces the [permissions] and their granted status into a
 * [PermissionsCache].
 */
private fun Context.permissionsCacheStateHolder(
    permissions: List<String>
) = object : ActionStateMutator<String, StateFlow<PermissionsCache>> {
    val stateFlow = MutableStateFlow(PermissionsCache())

    init {
        onActivitiesChanged { activityCache ->
            val lastChangedActivity = activityCache.lastChangedActivity
                ?: return@onActivitiesChanged
            val permissionCache = stateFlow.value

            stateFlow.value = when (activityCache.latestEvent) {
                Lifecycle.Event.ON_CREATE -> permissionCache.copy(
                    activeActivity = lastChangedActivity,
                    map = permissionCache.map + (lastChangedActivity to permissions.map { permission ->
                        permission to lastChangedActivity.permissionStateHolder(permission)
                    }.toMap())
                )

                Lifecycle.Event.ON_DESTROY -> permissionCache.copy(
                    activeActivity = when (lastChangedActivity) {
                        permissionCache.activeActivity -> null
                        else -> lastChangedActivity
                    },
                    map = permissionCache.map - lastChangedActivity
                )

                Lifecycle.Event.ON_RESUME -> permissionCache.copy(activeActivity = lastChangedActivity)
                Lifecycle.Event.ON_START -> permissionCache.copy(activeActivity = lastChangedActivity)
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> when (lastChangedActivity) {
                    permissionCache.activeActivity -> permissionCache.copy(activeActivity = null)
                    else -> permissionCache
                }

                Lifecycle.Event.ON_ANY -> permissionCache
            }
        }
    }

    override val state: StateFlow<PermissionsCache> = stateFlow

    override val accept: (String) -> Unit = { permission ->
        val cache = stateFlow.value
        cache.activeActivity
            ?.let(cache.map::get)
            ?.get(permission)
            ?.accept?.invoke(Unit)
    }
}

/**
 * Creates a [StateHolder] whose output is the state of a single [permissionString]'s granted status.
 */
private fun ComponentActivity.permissionStateHolder(permissionString: String): ActionStateMutator<Unit, StateFlow<Boolean>> {
    val permissionStateFlow = MutableStateFlow(hasPermission(permissionString))
    val isActiveStateFlow = MutableStateFlow(lifecycle.currentState != Lifecycle.State.DESTROYED)

    val launcher: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        permissionStateFlow::value::set
    )

    // Check the status of the permission on every lifecycle event
    lifecycle.addObserver(LifecycleEventObserver { _, event ->
        permissionStateFlow.value = hasPermission(permissionString)
        isActiveStateFlow.value = event != Lifecycle.Event.ON_DESTROY
    })

    return object : ActionStateMutator<Unit, StateFlow<Boolean>> {
        override val state: StateFlow<Boolean> = combine(
            permissionStateFlow,
            isActiveStateFlow,
            ::Pair
        )
            // Stop collecting from this [Flow] when the [Activity] is destroyed
            .transformWhile { (hasPermission, isActive) ->
                emit(hasPermission)
                isActive
            }
            .stateIn(
                initialValue = hasPermission(permissionString),
                scope = lifecycleScope,
                started = SharingStarted.Eagerly
            )

        override val accept: (Unit) -> Unit = { launcher.launch(permissionString) }
    }
}

private fun ComponentActivity.hasPermission(permissionString: String) =
    ContextCompat.checkSelfPermission(
        this,
        permissionString
    ) == PackageManager.PERMISSION_GRANTED

private val String.toPermission: Permission?
    get() = when (this) {
        Manifest.permission.READ_EXTERNAL_STORAGE -> Permission.ReadExternalStorage
        else -> null
    }

private val Permission.toManifestString: String?
    get() = when (this) {
        Permission.ReadExternalStorage -> Manifest.permission.READ_EXTERNAL_STORAGE
        else -> null
    }