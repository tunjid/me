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
    id("com.android.library")
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization") version "1.6.10"
    id("com.squareup.sqldelight")
}

kotlin {
    android()
    jvm("desktop")
    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(libs.compose.ui.tooling)
                implementation(libs.compose.ui.util)

                implementation(libs.compose.runtime)
                implementation(libs.compose.animation)
                implementation(libs.compose.material)
                implementation(libs.compose.foundation.layout)

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
                implementation(libs.arkivanov.essenty.parcelable)

                implementation(libs.accompanist.flowlayout)

                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.appcompat)

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
                implementation(libs.ktor.client.java)

                implementation(libs.square.sqldelight.driver.jvm)
            }
        }
        named("commonTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        all {
            languageSettings.apply {
                optIn("androidx.compose.animation.ExperimentalAnimationApi")
                optIn("androidx.compose.foundation.ExperimentalFoundationApi")
                optIn("androidx.compose.material.ExperimentalMaterialApi")
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlinx.coroutines.FlowPreview")
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

android {
    compileSdk = 31

    defaultConfig {
        // Could have been 21, but I need sqlite 3.24.0 for upserts
        minSdk = 30
        targetSdk = 31
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    sourceSets {
        named("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            res.srcDirs("src/androidMain/res")
        }
    }
}

