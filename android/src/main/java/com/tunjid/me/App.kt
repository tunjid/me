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
import com.tunjid.me.common.app.AppAction
import com.tunjid.me.common.SavedState
import com.tunjid.me.common.app.createAppDependencies
import com.tunjid.me.common.data.AppDatabase
import com.tunjid.me.common.data.local.DatabaseDriverFactory
import com.tunjid.me.common.data.network.NetworkMonitor
import com.tunjid.me.common.data.fromBytes
import com.tunjid.me.common.data.toBytes
import com.tunjid.me.common.restore
import com.tunjid.me.common.saveState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private const val SavedStateKey = "com.tunjid.me.android_saved_state"

class App : Application() {

    val appDependencies by lazy {
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        createAppDependencies(
            appScope = appScope,
            networkMonitor = NetworkMonitor(scope = appScope, context = this),
            database = AppDatabase.invoke(DatabaseDriverFactory(this).createDriver())
        )
    }

    override fun onCreate() {
        super.onCreate()

        val appMutator = appDependencies.appMutator
        val byteSerializer = appDependencies.byteSerializer

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private fun updateStatus(isInForeground: Boolean) =
                appMutator.accept(AppAction.AppStatus(isInForeground = isInForeground))

            override fun onActivityCreated(p0: Activity, bundle: Bundle?) {
                bundle?.getByteArray(SavedStateKey)
                    ?.let<ByteArray, SavedState>(byteSerializer::fromBytes)
                    ?.let(appDependencies::restore)

                updateStatus(isInForeground = true)
            }

            override fun onActivityStarted(p0: Activity) =
                updateStatus(isInForeground = true)

            override fun onActivityResumed(p0: Activity) =
                updateStatus(isInForeground = true)

            override fun onActivityPaused(p0: Activity) =
                updateStatus(isInForeground = false)

            override fun onActivityStopped(p0: Activity) =
                updateStatus(isInForeground = false)

            override fun onActivitySaveInstanceState(p0: Activity, bundle: Bundle) {
                bundle.putByteArray(
                    SavedStateKey,
                    byteSerializer.toBytes(appDependencies.saveState())
                )
            }

            override fun onActivityDestroyed(p0: Activity) =
                updateStatus(isInForeground = false)
        })
    }
}
