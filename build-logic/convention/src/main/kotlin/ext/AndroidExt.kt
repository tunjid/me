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

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

/**
 * Sets common values for Android Applications and Libraries
 */
fun org.gradle.api.Project.commonConfiguration(
    extension: CommonExtension<*, *, *, *>
) = extension.apply {
    compileSdk = 33

    defaultConfig {
        // Could have been 21, but I need sqlite 3.24.0 for upserts
        minSdk = 30
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        val composeCompilerVersion = versionCatalog
            .findVersion("androidxComposeCompiler")
            .get()
            .requiredVersion
        println("USING VERSION $composeCompilerVersion")
        kotlinCompilerExtensionVersion = composeCompilerVersion
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    (this as org.gradle.api.plugins.ExtensionAware).apply {
        val kotlinOptions = "kotlinOptions"
        if (extensions.findByName(kotlinOptions) != null) {
            extensions.configure(kotlinOptions, Action<KotlinJvmOptions> {
                jvmTarget = "11"
            })
        }
    }
}

fun org.gradle.api.Project.coerceComposeVersion(configuration: Configuration) {
    val independentGroups = setOf("compiler", "material3")
    configuration.resolutionStrategy.eachDependency {
        if (requested.group.startsWith("androidx.compose") && independentGroups.none(requested.group::contains)) {
            useVersion(versionCatalog.findVersion("androidxCompose").get().requiredVersion)
            because("I need the changes in lazyGrid")
        }
    }
}

fun org.gradle.api.Project.addDesugarDependencies() {
    dependencies {
        add(
            configurationName = "coreLibraryDesugaring",
            dependencyNotation = versionCatalog.findLibrary("android-desugarJdkLibs").get()
        )
    }
}

val org.gradle.api.Project.versionCatalog
    get() = extensions.getByType(VersionCatalogsExtension::class.java)
        .named("libs")