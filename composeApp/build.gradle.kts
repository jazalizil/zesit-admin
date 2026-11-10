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
            // API des ressources Compose (Res.font.* Montserrat) pour le code généré.
            implementation(compose.components.resources)
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
            // EncryptedSharedPreferences pour le stockage sécurisé du token ntfy
            implementation(libs.androidx.security.crypto)
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
        // Tests desktop : parseur desktop.env (DesktopEnvFile) + garde-fous d'URL (EnvFactory).
        named("desktopTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

compose {
    // Polices par défaut (Montserrat) : classe Res générée pour les ressources
    // composeResources/fonts/ — fontes versées dans les builds des 3 plateformes.
    resources {
        packageOfResClass = "fr.zesit.admin"
        generateResClass = always
    }

    desktop {
        application {
            mainClass = "fr.zesit.admin.MainKt"
            nativeDistributions {
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                packageName = "Zesit Admin"
                packageVersion = "1.0.0"
                description = "Administration des notifications ntfy (Zesit)"
                // Icône du .app/.dmg (macOS) — AppIcon.icns dérivé du même
                // artwork que les icônes iOS/Android.
                macOS {
                    iconFile.set(project.file("packaging/AppIcon.icns"))
                }
            }
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

        // Jetons/URLs d'env (AppEnv via EnvOverrides.kt/AndroidEnv.kt générés par
        // ./gradlew generateEnv) : un buildConfigField par clé de la table des
        // rôles (partagée par le build racine via extraProperties, union avec
        // les clés du env-android.properties généré). Priorité : propriété
        // Gradle > variable d'environnement > env-android.properties (ignoré par Git).
        val envRoleKeys: List<String> = rootProject.extensions.extraProperties
            .run { if (has("zesitEnvKeys")) get("zesitEnvKeys") else null }
            .let { keys -> (keys as? List<*>).orEmpty().map { it.toString() } }
        val generatedAndroidEnv: Map<String, String> = runCatching {
            val propsFile = file("env-android.properties")
            if (!propsFile.isFile) {
                emptyMap()
            } else {
                propsFile.readLines().mapNotNull { line ->
                    val trimmed = line.trim()
                    val sep = trimmed.indexOfFirst { it == '=' }
                    if (trimmed.isEmpty() || trimmed.startsWith("#") || sep <= 0) {
                        null
                    } else {
                        trimmed.substring(0, sep).trim() to trimmed.substring(sep + 1).trim()
                    }
                }.toMap()
            }
        }.getOrDefault(emptyMap())
        fun envOrGenerated(key: String): String =
            providers.gradleProperty(key)
                .orElse(providers.environmentVariable(key))
                .orElse(providers.provider { generatedAndroidEnv[key] })
                .getOrElse("")
        (envRoleKeys + generatedAndroidEnv.keys).distinct().forEach { key ->
            buildConfigField("String", key, "\"" + envOrGenerated(key) + "\"")
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // AppEnv Android lit BuildConfig.DEBUG / VERSION_NAME (EnvFactory.android).
    buildFeatures {
        buildConfig = true
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
