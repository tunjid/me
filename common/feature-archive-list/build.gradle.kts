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
    `feature-module-convention`
}

kotlin {
    sourceSets {
        named("commonMain") {
            dependencies {
                api(project(":common:core-ui"))
                implementation(project(":common:data"))
                implementation(project(":common:scaffold"))
                implementation(project(":common:feature-template"))

                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.jetbrains.compose.animation)
                implementation(libs.jetbrains.compose.material)
                implementation(libs.jetbrains.compose.foundation.layout)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.tunjid.tiler)

                implementation(libs.tunjid.mutator.core.common)
                implementation(libs.tunjid.mutator.coroutines.common)

                implementation(libs.tunjid.treenav.common)
            }
        }
        named("commonTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.cashapp.turbine)
                implementation(libs.mockk)
            }
        }
    }
}

