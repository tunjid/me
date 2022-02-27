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
    kotlin("plugin.serialization")
    id("com.squareup.sqldelight")
}

kotlin {
    android()
    jvm("desktop")
    sourceSets {
        named("commonMain") {
            dependencies {
                api(project(":common:core"))

                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.ktor.core)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.serialization)

                implementation(libs.square.sqldelight.coroutines.extensions)

                implementation(libs.tunjid.tiler)
                implementation(libs.tunjid.mutator.core.common)
                implementation(libs.tunjid.mutator.coroutines.common)
            }
        }
        named("androidMain") {
            dependencies {
                implementation(project(":serverEvents"))

                implementation(libs.androidx.core.ktx)

                implementation(libs.ktor.client.android)

                implementation(libs.square.sqldelight.driver.android)
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

sqldelight {
    database("AppDatabase") {
        dialect = "sqlite:3.25"
        packageName = "com.tunjid.me.common.data"
        schemaOutputDirectory = file("build/dbs")
    }
}
