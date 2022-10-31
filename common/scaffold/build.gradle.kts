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

plugins {
    `android-library-convention`
    `kotlin-library-convention`
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

kotlin {
    sourceSets {
        named("commonMain") {
            dependencies {
                api(project(":common:core"))

                implementation(libs.androidx.datastore.core.okio)

                implementation(libs.jetbrains.compose.ui.tooling)
                implementation(libs.jetbrains.compose.ui.util)
                implementation(libs.jetbrains.compose.ui.ui)

                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.jetbrains.compose.animation)
                implementation(libs.jetbrains.compose.material)
                implementation(libs.jetbrains.compose.foundation.foundation)
                implementation(libs.jetbrains.compose.foundation.layout)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.cbor)

                implementation(libs.cashapp.sqldelight.coroutines.extensions)

                implementation(libs.squareup.okio)

                implementation(libs.tunjid.treenav.core.common)
                implementation(libs.tunjid.treenav.strings.common)

                implementation(libs.tunjid.mutator.core.common)
                implementation(libs.tunjid.mutator.coroutines.common)
            }
        }
        named("androidMain") {
            dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.appcompat)
                implementation(libs.androidx.lifecycle.ktx)
            }
        }
        named("desktopMain") {
            dependencies {
            }
        }
        named("commonTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

