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

package com.tunjid.me

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.tunjid.me.common.di.createAppDependencies
import com.tunjid.me.core.utilities.UriConverter
import com.tunjid.me.data.local.DatabaseDriverFactory
import com.tunjid.me.data.network.NetworkMonitor
import com.tunjid.me.scaffold.permissions.PlatformPermissionsProvider
import com.tunjid.mutator.mutation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toPath

class App : Application() {

    val appDependencies by lazy {
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        createAppDependencies(
            appScope = appScope,
            savedStatePath = filesDir.resolve("savedState").absolutePath.toPath(),
            permissionsProvider = PlatformPermissionsProvider(appScope = appScope, context = this),
            networkMonitor = NetworkMonitor(scope = appScope, context = this),
            uriConverter = UriConverter(),
            database = AppDatabase(
                DatabaseDriverFactory(
                    context = this,
                    schema = AppDatabase.Schema,
                ).createDriver()
            )
        )
    }

    override fun onCreate() {
        super.onCreate()

        val scaffoldComponent = appDependencies.scaffoldComponent

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private fun updateStatus(isInForeground: Boolean) =
                scaffoldComponent.lifecycleActions(mutation {
                    copy(isInForeground = isInForeground)
                })

            override fun onActivityCreated(p0: Activity, bundle: Bundle?) =
                updateStatus(isInForeground = true)

            override fun onActivityStarted(p0: Activity) =
                updateStatus(isInForeground = true)

            override fun onActivityResumed(p0: Activity) =
                updateStatus(isInForeground = true)

            override fun onActivityPaused(p0: Activity) =
                updateStatus(isInForeground = false)

            override fun onActivityStopped(p0: Activity) =
                updateStatus(isInForeground = false)

            override fun onActivitySaveInstanceState(p0: Activity, bundle: Bundle) = Unit

            override fun onActivityDestroyed(p0: Activity) =
                updateStatus(isInForeground = false)
        })
    }
}
