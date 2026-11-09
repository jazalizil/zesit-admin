package fr.zesit.admin.di

import android.os.Build
import fr.zesit.admin.BuildConfig

/**
 * Env Android : spécialise le commun avec la plateforme, le modèle d'appareil
 * et le debug/version issus du BuildConfig Gradle.
 */
actual fun platformEnv(): AppEnv = commonEnv().copy(
    platform = "android",
    debug = BuildConfig.DEBUG,
    version = BuildConfig.VERSION_NAME,
    deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}",
)
