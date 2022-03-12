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

package lift

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle

/**
 * A class that tracks every [Activity] in the application.
 *
 * Its utility is for hoisting to upper levels APIs that are typically only available in the UI layer
 */
internal data class ActivityStore(
    val latestEvent: Lifecycle.Event = Lifecycle.Event.ON_DESTROY,
    val eventToActivities: Map<Lifecycle.Event, List<ComponentActivity>> = Lifecycle.Event.values()
        // Do not keep references to destroyed activities
        .filter { it != Lifecycle.Event.ON_DESTROY && it != Lifecycle.Event.ON_ANY }
        .fold(mutableMapOf()) { map, event ->
            map[event] = listOf()
            map
        }
)


internal fun Context.onActivitiesChanged(onChanged: (ActivityStore) -> Unit) {
    var cache = ActivityStore()
    fun reduce(event: Lifecycle.Event, activity: Activity) {
        if (activity !is ComponentActivity) return

        cache = cache.copy(
            latestEvent = event,
            eventToActivities = cache.eventToActivities.mapValues { (key, value) ->
                // Keep the most recently changed activity at the end of the list
                if (key == event) (value - activity) + activity
                else value - activity
            }
        )
        onChanged(cache)
    }

    val callbacks = object :
        Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) =
            reduce(Lifecycle.Event.ON_CREATE, activity)

        override fun onActivityStarted(activity: Activity) =
            reduce(Lifecycle.Event.ON_START, activity)

        override fun onActivityResumed(activity: Activity) =
            reduce(Lifecycle.Event.ON_RESUME, activity)

        override fun onActivityPaused(activity: Activity) =
            reduce(Lifecycle.Event.ON_PAUSE, activity)

        override fun onActivityStopped(activity: Activity) =
            reduce(Lifecycle.Event.ON_STOP, activity)

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

        override fun onActivityDestroyed(activity: Activity) =
            reduce(Lifecycle.Event.ON_DESTROY, activity)

    }
    val app = (applicationContext as Application)
    app.registerActivityLifecycleCallbacks(callbacks)
}

internal val ActivityStore.lastChangedActivity: ComponentActivity?
    get() {
        val activityList = eventToActivities[latestEvent]
        return if (activityList == null || activityList.isEmpty()) null
        else activityList.last()
    }