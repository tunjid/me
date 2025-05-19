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
    id("ksp-convention")
}
android {
    namespace = "com.tunjid.me.scaffold"
}

kotlin {
    sourceSets {
        named("commonMain") {
            dependencies {
                api(project(":common:core"))
                api(project(":common:core-ui"))
                api(project(":common:sync"))

                implementation(libs.androidx.datastore.core.okio)

                implementation(libs.lifecycle.runtime)
                implementation(libs.lifecycle.runtime.compose)
                implementation(libs.lifecycle.viewmodel)
                implementation(libs.lifecycle.viewmodel.compose)

                implementation(libs.compose.components.resources)

                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.compose.ui.util)
                implementation(libs.compose.ui.ui)

                implementation(libs.compose.runtime)
                implementation(libs.compose.animation)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.material3)
                implementation(libs.compose.foundation.foundation)
                implementation(libs.compose.foundation.layout)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.cbor)

                implementation(libs.cashapp.sqldelight.coroutines.extensions)

                implementation(libs.squareup.okio)

                implementation(libs.tunjid.composables)
                implementation(libs.tunjid.treenav.compose)
                implementation(libs.tunjid.treenav.compose.threepane)
                implementation(libs.tunjid.treenav.core)
                implementation(libs.tunjid.treenav.strings)

                implementation(libs.tunjid.mutator.core.common)
                implementation(libs.tunjid.mutator.coroutines.common)
            }
        }
        named("androidMain") {
            dependencies {
                api(project(":common:core"))
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.appcompat)
                implementation(libs.androidx.activity.compose)
            }
        }
        named("desktopMain") {
            dependencies {
                api(project(":common:core"))
            }
        }
        named("commonTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

