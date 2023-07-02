package ext

import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
* Configure base Kotlin options for JVM (non-Android)
*/
internal fun org.gradle.api.Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    configureKotlin()
}

/**
 * Configure base Kotlin options
 */
private fun org.gradle.api.Project.configureKotlin() {
    // Use withType to workaround https://youtrack.jetbrains.com/issue/KT-55947
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            // Set JVM target to 11
            jvmTarget = JavaVersion.VERSION_11.toString()
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-Xuse-experimental=androidx.compose.animation.ExperimentalAnimationApi",
                "-Xuse-experimental=androidx.compose.material.ExperimentalMaterialApi",
                "-Xuse-experimental=kotlinx.serialization.ExperimentalSerializationApi",
                "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-Xuse-experimental=kotlinx.coroutines.FlowPreview"
            )
        }
    }
    val kotlinOptions = "kotlinOptions"
    if (extensions.findByName(kotlinOptions) != null) {
        extensions.configure(kotlinOptions, Action<KotlinJvmOptions> {
            jvmTarget = JavaVersion.VERSION_11.toString()
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-Xuse-experimental=androidx.compose.animation.ExperimentalAnimationApi",
                "-Xuse-experimental=androidx.compose.material.ExperimentalMaterialApi",
                "-Xuse-experimental=kotlinx.serialization.ExperimentalSerializationApi",
                "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-Xuse-experimental=kotlinx.coroutines.FlowPreview"
            )
        })
    }
}