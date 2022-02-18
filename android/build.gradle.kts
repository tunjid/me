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
    id("com.android.application")
    id("kotlin-android")
    kotlin("plugin.serialization")
    id("kotlin-parcelize")
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "com.tunjid.me"
        // Could have been 21, but I need sqlite 3.24.0 for upserts
        minSdk = 30
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.2.0-alpha02"
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xuse-experimental=androidx.compose.animation.ExperimentalAnimationApi",
            "-Xuse-experimental=androidx.compose.material.ExperimentalMaterialApi",
            "-Xuse-experimental=kotlinx.serialization.ExperimentalSerializationApi",
            "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xuse-experimental=kotlinx.coroutines.FlowPreview"
        )
    }
}

dependencies {
    coreLibraryDesugaring(libs.android.desugarJdkLibs)

    implementation(project(":common:all"))

    implementation(libs.accompanist.flowlayout)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.window)

    implementation(libs.jetbrains.compose.foundation.layout)
    implementation(libs.jetbrains.compose.material)
    implementation(libs.jetbrains.compose.animation)
    implementation(libs.jetbrains.compose.runtime)
    implementation(libs.jetbrains.compose.ui.util)

//    val composeVersion = "1.2.0-alpha02"
//    implementation("androidx.compose.foundation:foundation:$composeVersion")
//    implementation("androidx.compose.material:material:$composeVersion")
//    implementation("androidx.compose.animation:animation:$composeVersion")
//    implementation("androidx.compose.runtime:runtime:$composeVersion")
//    implementation("androidx.compose.animation:animation:$composeVersion")


    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.google.material)

    implementation(libs.coil.core)
    implementation(libs.coil.compose)

    implementation(libs.richtext.commonmark)
    implementation(libs.richtext.material)

    implementation(libs.tunjid.mutator.core.jvm)
    implementation(libs.tunjid.mutator.coroutines.jvm)
    implementation(libs.tunjid.tiler)
    implementation(libs.tunjid.treenav.jvm)
}
