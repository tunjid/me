# Me

Please note, this is not an official Google repository. It is a Kotlin multiplatform experiment
that makes no guarantees about API stability or long term support. None of the works presented here
are production tested, and should not be taken as anything more than its face value.

## Introduction

"Me" is a playground for ideas that pop into my head around app architecture. This ideas typically
center around state and it's production. A repository of "what ifs?". Some of the ideas explored
include:

* Android insets and IME behavior driven by immutable state
* Tiling as a way of loading paginated data
* Trees as a backing data structure for app navigation
* `Mutators` as abstract data types for the production and mutation of state
* Motional intelligence with global UI as implemented with Jetpack Compose

I try to keep the code at a near production quality, but this often takes a back seat to
convenience and whim. I'm a huge proponent of dependency injection, yet the repository uses manual
service location. Also outside data classes, virtually everything else is implemented as a function,
or anonymous class just because.

Again, the work presented here are the ideas of an immutable state zealot. It's far from objective,
I just be doing anything tbh. Caveat emptor.