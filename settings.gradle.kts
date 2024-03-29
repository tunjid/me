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

pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://androidx.dev/snapshots/builds/11510162/artifacts/repository")
    }
}

rootProject.name = "me"
include(
    ":common:core",
    ":common:core-ui",
    ":common:data",
    ":common:scaffold",
    ":common:app",
    ":common:ui:template",
    ":common:ui:archive-list",
    ":common:ui:archive-detail",
    ":common:ui:archive-edit",
    ":common:ui:archive-files",
    ":common:ui:profile",
    ":common:ui:settings",
    ":common:ui:sign-in",
    ":common:sync",
    ":android",
    ":desktop",
    ":serverEvents"
)
