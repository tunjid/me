# Me

Please note, this is not an official Google repository. It is a Kotlin multiplatform experiment
that makes no guarantees about API stability or long term support. None of the works presented here
are production tested, and should not be taken as anything more than its face value.

## Introduction

"Me" is a Kotlin Multiplatform playground for ideas that pop into my head around app architecture.
These ideas typically center around state, and it's production; a repository of "what ifs?".

It follows the modern android development [architecture guide](https://developer.android.com/topic/architecture), and
attempts to extend it to envision what building apps will look like in the near future,
with novel/experimental implementations of fundamental app architecture units including:

* Navigation
* Pagination
* UI State production
* Dependency injection
* Persistent animation

All while targeting multiple platforms.

The app is a WYSIWYG editor for my personal website. The source for the backend can be
found [here](https://github.com/tunjid/tunji-web-deux).

![Demo image](https://github.com/tunjid/me/blob/main/misc/demo.gif)

Some ideas explored include:

* [Mutators](https://github.com/tunjid/Mutator) as abstract data types for the production and mutation of state
* Reactive app architecture as a driver of app state
* Android insets and IME (keyboard) behavior as state
* Android permissions as state
* [Tiling](https://github.com/tunjid/Tiler) for incremental loading (pagination) as state
* [Trees](https://github.com/tunjid/treeNav) for representing app navigation as state
* [Jetpack Compose](https://developer.android.com/jetpack/compose?gclid=CjwKCAiA4KaRBhBdEiwAZi1zzpXxpbbQ5-qXVpv8RHzJKKCDY_Yv7AXMLpeRHaMCK-SNVI9i4jvJ4RoC_VQQAvD_BwE&gclsrc=aw.ds)
  for
  stateful [motionally intelligent](https://medium.com/androiddevelopers/motional-intelligence-build-smarter-animations-821af4d5f8c0)
  global UI

🚨⚠️🚧👷🏿‍♂️🏗️🛠️🚨

I try to keep the code at a near production quality, but this often takes a back seat to
convenience and whim. I'm a huge proponent of dependency injection, yet the repository uses manual
service location.

Again, the work presented here are the experiments of an immutable state and functional reactive programming zealot.
It's far from objective, caveat emptor.

## Arch

### Reactive architecture

The app is a subscriber in a pub-sub liaison with the server. There is no pull to refresh, instead the app pulls diffs
of `ChangeListItem` when the server notifies the app of changes made.

The following rules are applied to the data layer:

* DAOs are internal to the data layer
* DAOs expose their data with reactive types (`Flow`)
* Reads from the data layer NEVER error.
* Writes to the data layer may error and the error is bubbled back up to the caller
* The `NetworkService` is internal to the data layer

#### Pub sub implementation

Pub sub in the app is backed by a change list invalidation based system. Its premise is:

* Each model table on the server will have a sibling table that has a row that tracks a unique id that identifies a CRUD
  update (change_list_id). This unique id must have natural ordering.
* CRUD updates to any model will cause an update for the change_list_id (akin to a new commit in git).
* The client will then hit an endpoint asking for changes since the last change_list_id it has, or its local HEAD. A
  changelist of model ids that have changed will then be sent (akin to a git fetch)
* The clients will then chew on the change list incrementally, updating its local HEAD as each update is consumed (akin
  to applying the pulled commits).

Real time updates are implemented with websockets via [socket.io](https://socket.io/). I intend to move the android
client to FCM for efficiency reasons in the future.

### Navigation

Each destination in the app is represented by an `AppRoute` that exposes a single `@Composable`
`Render()` function. The backing data structure for navigation is the tree like `StackNav` and
`MultiStackNav` immutable classes. The root of the app is a `MultiStackNav` and navigation is
controlled by a `NavMutator` defined as:

```
typealias NavMutator = ActionStateProducer<Mutation<MultiStackNav>, StateFlow<MultiStackNav>>
```

### Global UI

The app utilizes a single bottom nav, toolbar and a shared global UI state as defined by the
`UiState` class. This is what allows for the app to have responsive navigation while accounting
for visual semantic differences between Android and desktop. Android for example uses the
`WindowManager` API to drive it's responsiveness whereas desktop just watches it's `Window` size.
The definition for the `GlobalUiMutator` is:

```
typealias GlobalUiMutator = ActionStateProducer<Mutation<UiState>, StateFlow<UiState>>
```

### Pagination

Pagination is implemented as a function of the current page and grid size:

```
[out of bounds]                    -> Evict from memory
                                                   _
[currentPage - gridSize - gridSize]                 |
...                                                 | -> Keep pages in memory, but don't observe
[currentPage - gridSize - 1]   _                   _|                        
[currentPage - gridSize]        |
...                             |
[currentPage - 1]               |
[currentPage]                   |  -> Observe pages     
[currentPage + 1]               |
...                             |
[currentPage + gridSize]       _|                  _
[currentPage + gridSize + 1]                        |
...                                                 | -> Keep pages in memory, but don't observe
[currentPage + gridSize + 1 + gridSize]            _|

[out of bounds]                    -> Evict from memory
```

As the user scrolls, `currentPage` changes and new pages are observed to keep the UI relevant.

### State restoration and process death

All types that need to be restored after process death implement the `ByteSerializable` interface.
This allows them to de serialized compactly into a `ByteArray` which works excellently with
Android's `Parcelable` type and a regular file system on Desktop. The bytes are read or written
with a type called the `ByteSerializer`.

Things restored after process death currently include:

* App navigation
* The state of each `AppRoute` at the time of process death

### Lifecycles and component scoping

Lifecycles are one of the most important concepts on Android, however Jetpack Compose itself is
pretty binary; a `Composable` is either in composition or not. Trying to expand this simplicity to
Android; the app may either be in the foreground or not.

The application state is defined in the `ScaffoldModule`:

```
class ScaffoldModule(
    appScope: CoroutineScope,
    ...
) {
    val navMutator: ActionStateProducer<Mutation<MultiStackNav>, StateFlow<MultiStackNav>> = ...
    val globalUiMutator: ActionStateProducer<Mutation<UiState>, StateFlow<UiState>> = ...
    val lifecycleMutator: ActionStateProducer<LifecycleAction, StateFlow<Lifecycle>> = ...
}
```

where `Lifecycle` is:

```
data class Lifecycle(
    val isInForeground: Boolean = true,
    val routeIdsToSerializedStates: Map<String, ByteArray> = mapOf()
)
```

With the above managing the lifecycle of components scoped to navigation destinations becomes as
easy as:

```
internal class RouteMutatorFactory(
    appScope: CoroutineScope,
    private val scaffoldComponent: ScaffoldComponent,
    ...
) : RouteServiceLocator {
    init {
        appScope.launch {
            scaffoldComponent
                .navStateStream
                .removedRoutes()
                .collect { removedRoutes ->
                    removedRoutes.forEach { route ->
                        val holder = routeMutatorCache.remove(route)
                        holder?.scope?.cancel()
                    }
                }
        }
    }
}
```

By watching the changes to the available routes in the `MultiStackNav` class, the scopes for
`Mutators` for routes that have been removed can be cancelled.

Each navigation destination can also be informed when the app is in the background via the
`isInForeground` field on `AppState`. It's `Mutator` can then opt to terminate it's backing flow
if necessary.

## Running

As this is a multiplatform app, syntax highlighting may be broken in Android studio. You may fare
better building with Intellij.

Desktop: `./gradlew :desktop:run`
Android: `./gradlew :android:assembleDebug` or run the Android target in Android Studio

## License

    Copyright 2021 Google LLC

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
