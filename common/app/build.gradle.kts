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
    id("app.cash.sqldelight")
}
android {
    namespace = "com.tunjid.me.common"
}

kotlin {
    sourceSets {
        named("commonMain") {
            dependencies {
                api(project(":common:core"))

                api(project(":common:data"))
                api(project(":common:scaffold"))
                api(project(":common:sync"))

                api(project(":common:ui:template"))
                api(project(":common:ui:archive-list"))
                api(project(":common:ui:archive-detail"))
                api(project(":common:ui:archive-edit"))
                api(project(":common:ui:archive-files"))
                api(project(":common:ui:profile"))
                api(project(":common:ui:settings"))
                api(project(":common:ui:sign-in"))

                implementation(libs.compose.material3)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.protobuf)

                implementation(libs.squareup.okio)

                api(libs.tunjid.mutator.core.common)
                api(libs.tunjid.treenav.core)
            }
        }
        named("androidMain") {
            dependencies {
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

sqldelight {
    databases {
        create("AppDatabase") {
            dialect(libs.cashapp.sqldelight.dialect)
            packageName.set("com.tunjid.me")
            schemaOutputDirectory.set(file("build/dbs"))
            deriveSchemaFromMigrations.set(false)
            verifyMigrations.set(true)
            dependency(project(":common:data"))
            dependency(project(":common:sync"))
        }
    }
}
