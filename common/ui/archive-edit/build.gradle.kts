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
    id("ui-module-convention")
}
android {
    namespace = "com.tunjid.me.feature.archiveedit"
}

kotlin {
    sourceSets {
        named("commonMain") {
            dependencies {
                api(project(":common:core-ui"))
                implementation(project(":common:data"))
                implementation(project(":common:scaffold"))
                implementation(project(":common:ui:template"))

                implementation(libs.compose.runtime)
                implementation(libs.compose.animation)
                implementation(libs.compose.material3)
                implementation(libs.compose.foundation.layout)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.ktor.io.core)

                implementation(libs.richtext.commonmark)
                implementation(libs.richtext.material3)

                implementation(libs.tunjid.tiler.tiler)

                implementation(libs.tunjid.mutator.core.common)
                implementation(libs.tunjid.mutator.coroutines.common)

            }
        }
    }
}

