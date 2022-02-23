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
    kotlin("plugin.serialization") version "1.6.10"
    id("com.squareup.sqldelight")
}

kotlin {
    sourceSets {
        named("commonMain") {
            dependencies {
                api(project(":common:core"))
                api(project(":common:core-ui"))
                api(project(":common:data"))
                api(project(":common:scaffold"))
                api(project(":common:feature-template"))
                api(project(":common:feature-archive-list"))

                implementation(libs.jetbrains.compose.ui.tooling)
                implementation(libs.jetbrains.compose.ui.util)

                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.jetbrains.compose.animation)
                implementation(libs.jetbrains.compose.material)
                implementation(libs.jetbrains.compose.foundation.layout)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.ktor.core)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.serialization)

                implementation(libs.richtext.commonmark)
                implementation(libs.richtext.material)

                implementation(libs.square.sqldelight.coroutines.extensions)

                implementation(libs.tunjid.tiler)
                implementation(libs.tunjid.treenav.common)
                implementation(libs.tunjid.mutator.core.common)
                implementation(libs.tunjid.mutator.coroutines.common)
            }
        }
        named("androidMain") {
            dependencies {
                implementation(project(":serverEvents"))

                implementation(libs.accompanist.flowlayout)

                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.appcompat)

                implementation(libs.androidx.compose.foundation.layout)

                implementation(libs.coil.compose)

                implementation(libs.kotlinx.coroutines.android)

                implementation(libs.ktor.client.android)

                implementation(libs.square.sqldelight.driver.android)

                implementation(libs.tunjid.mutator.core.jvm)
                implementation(libs.tunjid.mutator.coroutines.jvm)
            }
        }
        named("desktopMain") {
            dependencies {
                implementation(project(":serverEvents"))
                implementation(libs.socketio.client.jvm)

                implementation(libs.ktor.client.java)

                implementation(libs.square.sqldelight.driver.jvm)
            }
        }
        named("commonTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
