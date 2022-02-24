import gradle.kotlin.dsl.accessors._84dd20d1a49d379320fe96b7964013dd.kotlin
import gradle.kotlin.dsl.accessors._84dd20d1a49d379320fe96b7964013dd.sourceSets
import org.gradle.kotlin.dsl.withType

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

fun org.gradle.api.Project.configureKotlinMultiplatform() {
    kotlin {
        android()
        jvm("desktop")
        sourceSets {
            all {
                languageSettings.apply {
                    optIn("androidx.compose.animation.ExperimentalAnimationApi")
                    optIn("androidx.compose.foundation.ExperimentalFoundationApi")
                    optIn("androidx.compose.material.ExperimentalMaterialApi")
                    optIn("androidx.compose.ui.ExperimentalComposeUiApi")
                    optIn("kotlinx.serialization.ExperimentalSerializationApi")
                    optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                    optIn("kotlinx.coroutines.FlowPreview")
                }
            }
        }
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.jvmTarget = "11"
        }
        configurations.all {
            coerceComposeVersion()
        }
    }
}