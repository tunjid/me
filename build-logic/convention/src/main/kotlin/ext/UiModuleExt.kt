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


import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun org.gradle.api.Project.configureUiModule(
    extension: KotlinMultiplatformExtension
) = extension.apply {
    sourceSets.apply {
        val catalogs = extensions.getByType(VersionCatalogsExtension::class.java)
        val libs = catalogs.named("libs")

        named("commonMain") {
            dependencies {
                implementation(project(":common:data"))
                implementation(project(":common:scaffold"))
                implementation(project(":common:ui:template"))

                implementation(libs.findLibrary("jetbrains-compose-runtime").get())
                implementation(libs.findLibrary("jetbrains-compose-animation").get())
                implementation(libs.findLibrary("jetbrains-compose-material3").get())
                implementation(libs.findLibrary("jetbrains-compose-foundation-layout").get())

                implementation(libs.findLibrary("kotlinx-coroutines-core").get())
                api(libs.findLibrary("tunjid-treenav-compose-common").get())
                api(libs.findLibrary("tunjid-treenav-core-common").get())
                api(libs.findLibrary("tunjid-treenav-strings-common").get())
            }
        }
        named("androidMain") {
            dependencies {
                implementation(libs.findLibrary("androidx-compose-foundation-layout").get())
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