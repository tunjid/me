# Me

Please note, this is not an official Google repository. It is a Kotlin multiplatform experiment
that makes no guarantees about API stability or long term support. None of the works presented here
are production tested, and should not be taken as anything more than its face value.

## Introduction

"Me" is a Kotlin Multiplatform playground for ideas that pop into my head around app architecture.
These ideas typically center around state and it's production; a repository of "what ifs?".
Some of the ideas explored include:

* Android insets and IME behavior driven by immutable state
* [Tiling](https://github.com/tunjid/Tiler) as a way of loading paginated data
* [Trees](https://github.com/tunjid/treeNav) as a backing data structure for app navigation
* [`Mutators`](https://github.com/tunjid/Mutator) as abstract data types for the production and mutation of state
* [Motional Intelligence](https://medium.com/androiddevelopers/motional-intelligence-build-smarter-animations-821af4d5f8c0) with global UI as implemented with Jetpack Compose


![Demo image](https://github.com/tunjid/me/blob/main/misc/demo.gif)

The API consumed is that of my personal website. The source can be found [here](https://github.com/tunjid/tunji-web-deux).

## Arch

### Navigation

Each destination in the app is represented by an `AppRoute` that exposes a single `@Composable`
`Render()` function. The backing data structures for navigation a the tree like `StackNav` and
`MultiStackNav` immutable classes. The root of the app is a `MultiStackNav` and navigation is
controlled by a `NavMutator` defined as:

```
typealias NavMutator = Mutator<Mutation<MultiStackNav>, StateFlow<MultiStackNav>>
```

### Global UI

The app utilizes a single bottom nav, toolbar and a shared global UI state as defined by the
`UiState` class. This is what allows for the app to have responsive navigation while accounting
for visual semantic differences between Android and desktop. Android for example uses the
`WindowManager` API to drive it's responsiveness whereas desktop just watches it's `Window` size.
The definition for the `GlobalUiMutator` is:

```
typealias GlobalUiMutator = Mutator<Mutation<UiState>, StateFlow<UiState>>
```

### State restoration and process death

The project does not currently take this into consideration as this time.
I'm still trying to figure this out in a scalable multiplatform way.


### Lifecycles and component scoping

Lifecycles are one of the most important concepts on Android, however Jetpack Compose itself is
pretty binary; a `Composable` is either in composition or not. Trying to expand this simplicity to
Android; the app may either be in the foreground or not. Therefore, the state of the app so far
can be represented as:

```
data class AppState(
    val nav: MultiStackNav,
    val ui: UiState,
    val isInForeground: Boolean = true
)
```

With the above managing the lifecycle of components scoped to navigation destinations becomes as
easy as:
```
private data class ScopeHolder(
    val scope: CoroutineScope,
    val mutator: Any
)

object: AppDependencies {
    init {
        scope.launch {
            appMutator.state
                .map { it.nav.routes.filterIsInstance<AppRoute<*>>() }
                .distinctUntilChanged()
                .scan(listOf<AppRoute<*>>() to listOf<AppRoute<*>>()) { pair, newRoutes ->
                    pair.copy(first = pair.second, second = newRoutes)
                }
                .distinctUntilChanged()
                .collect { (oldRoutes, newRoutes) ->
                    oldRoutes.minus(newRoutes.toSet()).forEach { route ->
                        val holder = routeMutatorFactory.remove(route)
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

I try to keep the code at a near production quality, but this often takes a back seat to
convenience and whim. I'm a huge proponent of dependency injection, yet the repository uses manual
service location. Also outside data classes, virtually everything else is implemented as a function,
or anonymous class just because.

Again, the work presented here are the ideas of an immutable state zealot. It's far from objective,
I just be doing anything tbh. Caveat emptor.

## Running
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
