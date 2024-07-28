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
    id("android-library-convention")
    id("kotlin-library-convention")
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}
android {
    namespace = "com.tunjid.me.core"
}

kotlin {
    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(libs.jetbrains.compose.ui.ui)
                implementation(libs.jetbrains.compose.ui.util)
                implementation(libs.jetbrains.compose.runtime)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.ktor.io.core)
            }
        }
        named("androidMain") {
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
            }
        }
        named("desktopMain") {
            dependencies {
                implementation(libs.ktor.io.jvm)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

