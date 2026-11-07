import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // En Kotlin 2.2, les cibles iOS ne produisent plus de framework binaire par
    // défaut (remplacé par le Swift Export). On le déclare explicitement : le projet
    // Xcode (iosApp) linke ComposeApp.framework depuis composeApp/build/xcode-frameworks.
    iosX64 {
        binaries.framework {
            baseName = "ComposeApp"
        }
    }
    iosArm64 {
        binaries.framework {
            baseName = "ComposeApp"
        }
    }
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "ComposeApp"
        }
    }

    // Cible desktop : fenêtre Compose Multiplatform autonome (équivalent macOS de l'app iOS)
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // api (et pas implementation) : le framework ComposeApp doit exposer
            // dans son header ObjC les déclarations publiques de shared, notamment
            // ApnsBridge (appelé depuis l'AppDelegate Swift).
            api(project(":shared"))
            implementation(libs.ktor.client.core)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        named("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                // Fournit Dispatchers.Main sur JVM desktop (Swing) — requis par viewModelScope
                implementation(libs.kotlinx.coroutines.swing)
                // Provider SLF4J pour faire taire le warning "No SLF4J providers were found"
                implementation(libs.slf4j.simple)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "fr.zesit.admin.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Zesit Admin"
            packageVersion = "1.0.0"
            description = "Administration des notifications ntfy (Zesit)"
        }
    }
}

android {
    namespace = "fr.zesit.admin"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "fr.zesit.admin"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
