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
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}
android {
    namespace = "com.tunjid.me.core.ui"
}

kotlin {
    sourceSets {
        named("commonMain") {
            dependencies {
                api(project(":common:core"))

                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.jetbrains.compose.animation)
                implementation(libs.jetbrains.compose.material3)
                implementation(libs.jetbrains.compose.foundation.layout)
                implementation(libs.jetbrains.compose.ui.util)

                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        named("androidMain") {
            dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(libs.accompanist.flowlayout)
                implementation(libs.androidx.compose.foundation.layout)

                implementation(libs.coil.compose)
            }
        }
        named("desktopMain") {
            dependencies {
                implementation(libs.ktor.client.java)
                implementation(libs.apache.commons.imaging)
                implementation(libs.image.io)
            }
        }
        named("commonTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

