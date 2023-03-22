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
* Large screen experiences

All while targeting multiple platforms and meeting stringent product requirement constraints like:

* No pull to refresh in the app. The app is always up to date.
* Navigation is persisted between app restarts and device reboots.
* Scroll position is preserved between app restarts and device reboots.

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
convenience and whim.

Again, the work presented here are the experiments of an immutable state and functional reactive programming zealot.
It's far from objective, caveat emptor.

## Architecture

### Data layer

#### Offline-first

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

#### Models

The app offers 3 main data types:

* `Archive`: Content I've produced over the years: Articles, projects and talks.
* `User`: The creator of the content shown. This is really just me.
* `SavedState`: App saved state. This is navigation state, and screen sate of each navigation destination.

### Domain Layer

The domain layer offers abstractions that consolidate common patterns and business logic across each future and its screen. There are two main types here:

* `NavStateHolder`: Manages the app navigation state and interacts with the `SavedStateRepository`. It provides Navigation as state.
* `GlobalUiStateHolder`: Manages configuration for the app and adapts it over different form factors and screen sizes. It provides app level UI as state.

### UI Layer

#### State production

All screen level state holders are implemented with [unidirectional data flow as a functional declaration](https://www.tunjid.com/articles/unidirectional-data-flow-as-a-functional-declaration-6230b74f5d785a7ebc8c2a43).

### X as state

#### Navigation as state

This app treats navigation as state, and as such, it is completely managed by business logic. The Navigation state
is persisted in the data layer with the `SavedStateRepository` and exposed to the app via the `NavStateHolder`.

Each destination in the app is represented by an `AppRoute` that exposes a single `@Composable`
`Render()` function. The backing data structures for navigation are the tree like [`StackNav`](https://github.com/tunjid/treeNav/blob/develop/treenav/src/commonMain/kotlin/com/tunjid/treenav/StackNav.kt) and
[`MultiStackNav`](https://github.com/tunjid/treeNav/blob/develop/treenav/src/commonMain/kotlin/com/tunjid/treenav/MultiStackNav.kt)
immutable classes. The root of the app is a `MultiStackNav` and navigation is
controlled by a `NavStateHolder` defined as:

```
typealias NavStateHolder = ActionStateProducer<Mutation<MultiStackNav>, StateFlow<MultiStackNav>>
```

#### Global UI as state

The app utilizes a single bottom nav, toolbar and a shared global UI state as defined by the
`UiState` class. This is what allows for the app to have responsive navigation while accounting
for visual semantic differences between Android and desktop. Android for example uses the
`WindowManager` API to drive it's responsiveness whereas desktop just watches it's `Window` size.
The definition for the `GlobalUiStateHolder` is:

```
typealias GlobalUiStateHolder = ActionStateProducer<Mutation<UiState>, StateFlow<UiState>>
```

#### Paging as state

Pagination is implemented as a function of the current page and number of columns in the grid:

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

#### State restoration and process death

All types that need to be restored after process death implement the `ByteSerializable` interface.
This allows them to de serialized compactly into a `ByteArray` which can then be saved to disk with a
[`DataStore`](https://developer.android.com/topic/libraries/architecture/datastore?gclid=CjwKCAjwtp2bBhAGEiwAOZZTuOs3XNmaNxY65HGo2wnRPqvKt1c18A1dhe4sETq_A3Iyx8DDv6uA1xoCv9kQAvD_BwE&gclsrc=aw.ds)
instance. The bytes are read or written with a type called the `ByteSerializer`.

Things restored after process death currently include:

* App navigation
* The state of each `AppRoute` at the time of process death

#### Lifecycles and component scoping

Screen state holders are scoped to the navigation state. When a route is removed from the navigation state, it's
state holder has it's `CoroutineScope` cancelled:

```kotlin
appScope.launch {
            navStateStream
                .map { it.mainNav }
                .removedRoutes()
                .collect { removedRoutes ->
                    removedRoutes.forEach { route ->
                        println("Cleared ${route::class.simpleName}")
                        val holder = routeStateHolderCache.remove(route)
                        holder?.scope?.cancel()
                    }
                }
        }
```

Lifecycles aware state collection is done with a custom `collectAsStateWithLifecycle` backed by the following lifecycle
definition:

```kotlin
data class Lifecycle(
    val isInForeground: Boolean = true,
)
```

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
