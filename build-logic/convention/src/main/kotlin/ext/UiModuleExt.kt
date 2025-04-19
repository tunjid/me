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


import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun org.gradle.api.Project.configureUiModule(
    extension: KotlinMultiplatformExtension
) = extension.apply {
    sourceSets.apply {
        named("commonMain") {
            dependencies {
                implementation(project(":common:data"))
                implementation(project(":common:scaffold"))
                implementation(project(":common:ui:template"))

                api(versionCatalog.findLibrary("lifecycle-runtime").get())
                api(versionCatalog.findLibrary("lifecycle-runtime-compose").get())
                api(versionCatalog.findLibrary("lifecycle-viewmodel").get())
                api(versionCatalog.findLibrary("lifecycle-viewmodel-compose").get())

                implementation(versionCatalog.findLibrary("compose-runtime").get())
                implementation(versionCatalog.findLibrary("compose-animation").get())
                implementation(versionCatalog.findLibrary("compose-material3").get())
                implementation(versionCatalog.findLibrary("compose-foundation-layout").get())

                implementation(versionCatalog.findLibrary("kotlinx-coroutines-core").get())
                api(versionCatalog.findLibrary("tunjid-treenav-compose-common").get())
                api(versionCatalog.findLibrary("tunjid-treenav-core-common").get())
                api(versionCatalog.findLibrary("tunjid-treenav-strings-common").get())
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
//        named("commonTest") {
//            dependencies {
//                implementation(kotlin("test"))
//            }
//        }
    }
}