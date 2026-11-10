package fr.zesit.admin.di

import android.os.Build
import fr.zesit.admin.BuildConfig

/**
 * Env Android : spécialise le commun avec la plateforme, le modèle d'appareil
 * et le debug/version issus du BuildConfig Gradle.
 *
 * Les autres variables d'env ([withEnvOverrides], généré par
 * ./gradlew generateEnv) sont résolues par [androidEnvLookup] (généré) depuis
 * les champs BuildConfig : propriété Gradle > variable d'environnement >
 * env-android.properties (lui-même généré).
 */
actual fun platformEnv(): AppEnv = commonEnv().copy(
    platform = "android",
    debug = BuildConfig.DEBUG,
    version = BuildConfig.VERSION_NAME,
    deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}",
).withEnvOverrides(::androidEnvLookup)
