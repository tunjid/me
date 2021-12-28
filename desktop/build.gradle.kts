import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") // kotlin("jvm") doesn't work well in IDEA/AndroidStudio (https://github.com/JetBrains/compose-jb/issues/22)
    id("org.jetbrains.compose")
}

kotlin {
    jvm {
        withJava()
    }

    sourceSets {
        named("jvmMain") {
            dependencies {
                implementation(project(":common"))

                implementation(compose.desktop.currentOs)

                implementation(libs.compose.material)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.tunjid.me.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "MeAsAComposition"
            packageVersion = "1.0.0"

            windows {
                menuGroup = "Compose Examples"
                // see https://wixtoolset.org/documentation/manual/v3/howtos/general/generate_guids.html
                upgradeUuid = "BF9CDA6A-1391-46D5-9ED5-383D6E68CCEB"
            }
        }
    }
}
