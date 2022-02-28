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

import java.util.Properties
import java.io.FileInputStream

plugins {
    `android-application-convention`
    id("kotlin-android")
    kotlin("plugin.serialization")
    id("kotlin-parcelize")
}

android {
    defaultConfig {
        applicationId = "com.tunjid.me"
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        getByName("debug") {
            if (file("debugKeystore.properties").exists()) {
                val props = Properties()
                props.load(FileInputStream(file("debugKeystore.properties")))
                storeFile = file(props["keystore"] as String)
                storePassword = props["keystore.password"] as String
                keyAlias = props["keyAlias"] as String
                keyPassword = props["keyPassword"] as String
            }
        }
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
    implementation(project(":common:app"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.window)

    implementation(libs.jetbrains.compose.foundation.layout)
    implementation(libs.jetbrains.compose.material)
    implementation(libs.jetbrains.compose.animation)
    implementation(libs.jetbrains.compose.runtime)
    implementation(libs.jetbrains.compose.ui.util)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.google.material)

    implementation(libs.coil.core)
    implementation(libs.coil.compose)

    implementation(libs.tunjid.mutator.core.jvm)
    implementation(libs.tunjid.mutator.coroutines.jvm)
    implementation(libs.tunjid.tiler)
    implementation(libs.tunjid.treenav.jvm)
}
