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
import com.tunjid.me.common.AppAction
import com.tunjid.me.common.AppMutator
import com.tunjid.me.common.createAppDependencies
import com.tunjid.me.common.data.AppDatabase
import com.tunjid.me.common.data.ByteSerializable
import com.tunjid.me.common.data.DatabaseDriverFactory
import com.tunjid.me.common.data.NetworkMonitor
import com.tunjid.me.common.data.bytes
import com.tunjid.me.common.nav.ByteSerializableNav
import com.tunjid.me.common.nav.NavName
import com.tunjid.me.common.nav.toByteSerializable
import com.tunjid.me.common.nav.toMultiStackNav
import com.tunjid.mutator.accept
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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
        watchActivityLifecycle(appDependencies.appMutator)
    }

    private fun watchActivityLifecycle(appMutator: AppMutator) {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private fun updateStatus(isInForeground: Boolean) =
                appMutator.accept(AppAction.AppStatus(isInForeground = isInForeground))

            override fun onActivityCreated(p0: Activity, bundle: Bundle?) {
                updateStatus(isInForeground = true)
                val serializedNav: ByteSerializableNav? = bundle
                    ?.getByteArray(NavName)
                    ?.let(ByteSerializable.Companion::fromBytes)

                if (serializedNav != null) appMutator.navMutator.accept {
                    serializedNav.toMultiStackNav
                }
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
                    NavName,
                    appMutator.navMutator.state.value.toByteSerializable.bytes()
                )
            }

            override fun onActivityDestroyed(p0: Activity) =
                updateStatus(isInForeground = false)
        })
    }
}
