plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization") version "1.6.10"
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
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.ktor.core)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.serialization)

                implementation(libs.richtext.commonmark)
                implementation(libs.richtext.material)

                implementation(libs.tunjid.tiler)
                implementation(libs.tunjid.treenav)
                implementation(libs.tunjid.mutator.core.common)
                implementation(libs.tunjid.mutator.coroutines.common)
            }
        }
        named("androidMain") {
            dependencies {
                implementation(libs.accompanist.flowlayout)

                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.appcompat)

                implementation(libs.coil.compose)

                implementation(libs.kotlinx.coroutines.android)

                implementation(libs.ktor.client.android)

                implementation(libs.tunjid.mutator.core.jvm)
                implementation(libs.tunjid.mutator.coroutines.jvm)
            }
        }
        named("desktopMain") {
            dependencies {
                implementation(libs.ktor.client.java)
            }
        }
        all {
            languageSettings.useExperimentalAnnotation("androidx.compose.animation.ExperimentalAnimationApi")
            languageSettings.useExperimentalAnnotation("androidx.compose.foundation.ExperimentalFoundationApi")
            languageSettings.useExperimentalAnnotation("androidx.compose.material.ExperimentalMaterialApi")
            languageSettings.useExperimentalAnnotation("kotlinx.serialization.ExperimentalSerializationApi")
            languageSettings.useExperimentalAnnotation("kotlinx.coroutines.ExperimentalCoroutinesApi")
            languageSettings.useExperimentalAnnotation("kotlinx.coroutines.FlowPreview")
        }
    }
}

android {
    compileSdk = 31

    defaultConfig {
        minSdk = 21
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

