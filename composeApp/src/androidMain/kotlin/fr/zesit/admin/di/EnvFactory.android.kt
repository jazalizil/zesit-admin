package fr.zesit.admin.di

import android.os.Build
import fr.zesit.admin.BuildConfig

/**
 * Env Android : spécialise le commun avec la plateforme, le modèle d'appareil
 * et le debug/version issus du BuildConfig Gradle. Le token TI est injecté par
 * la propriété Gradle `TI_API_TOKEN` (gradle.properties ou variable d'environnement).
 */
actual fun platformEnv(): AppEnv = commonEnv().copy(
    platform = "android",
    debug = BuildConfig.DEBUG,
    version = BuildConfig.VERSION_NAME,
    trafficToken = BuildConfig.TI_API_TOKEN.takeIf { it.isNotBlank() },
    deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}",
)
