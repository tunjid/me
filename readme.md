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
* Trees as a backing data structure for app navigation
* [`Mutators`](https://github.com/tunjid/Mutator) as abstract data types for the production and mutation of state
* [Motional Intelligence](https://medium.com/androiddevelopers/motional-intelligence-build-smarter-animations-821af4d5f8c0)with global UI as implemented with Jetpack Compose


![Demo image](https://github.com/tunjid/me/blob/main/misc/demo.gif)

The API consumed is that of my personal website. The source can be found [here](https://github.com/tunjid/tunji-web-deux).

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