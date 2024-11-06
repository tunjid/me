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
import com.tunjid.mutator.mutationOf

class App : Application() {

    val meApp by lazy { createMeApp(this) }

    override fun onCreate() {
        super.onCreate()

        val lifecycleActions = meApp.lifecycleStateHolder.accept

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private fun updateStatus(isInForeground: Boolean) =
                lifecycleActions(mutationOf {
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
