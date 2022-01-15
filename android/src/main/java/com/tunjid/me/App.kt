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
import com.tunjid.me.common.createAppDependencies
import com.tunjid.me.common.data.AppDatabase
import com.tunjid.me.common.data.ByteSerializable
import com.tunjid.me.common.data.DatabaseDriverFactory
import com.tunjid.me.common.data.NetworkMonitor
import com.tunjid.me.common.data.fromBytes
import com.tunjid.me.common.data.toBytes
import com.tunjid.me.common.nav.AppRoute
import com.tunjid.me.common.nav.ByteSerializableNav
import com.tunjid.me.common.nav.NavName
import com.tunjid.me.common.nav.toByteSerializable
import com.tunjid.me.common.nav.toMultiStackNav
import com.tunjid.mutator.Mutator
import com.tunjid.mutator.accept
import com.tunjid.treenav.Order
import com.tunjid.treenav.flatten
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow

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
                updateStatus(isInForeground = true)

                val serializedNav: ByteSerializableNav = bundle
                    ?.getByteArray(NavName)
                    ?.let(byteSerializer::fromBytes) ?: return

                val multiStackNav = serializedNav.toMultiStackNav

                appMutator.accept(
                    AppAction.RestoreSerializedStates(
                        routeIdsToSerializedStates = multiStackNav.flatten(Order.BreadthFirst)
                            .filterIsInstance<AppRoute<*>>()
                            .fold(mutableMapOf()) { map, route ->
                                bundle.getByteArray(route.id)?.let { map[route.id] = it }
                                map
                            }
                    )
                )
                appMutator.navMutator.accept { multiStackNav }
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
                val multiStackNav = appMutator.navMutator.state.value
                bundle.putByteArray(
                    NavName,
                    byteSerializer.toBytes(multiStackNav.toByteSerializable)
                )
                multiStackNav
                    .flatten(order = Order.BreadthFirst)
                    .filterIsInstance<AppRoute<*>>()
                    .forEach { route ->
                        val mutator = appDependencies.routeDependencies(route)
                        val state = (mutator as? Mutator<*, *>)?.state ?: return@forEach
                        val serializable = (state as? StateFlow<*>)?.value ?: return@forEach
                        if (serializable is ByteSerializable) bundle.putByteArray(
                            route.id,
                            byteSerializer.toBytes(serializable)
                        )
                    }
            }

            override fun onActivityDestroyed(p0: Activity) =
                updateStatus(isInForeground = false)
        })
    }
}
