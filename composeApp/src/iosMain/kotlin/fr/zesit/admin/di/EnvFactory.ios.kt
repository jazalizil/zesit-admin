package fr.zesit.admin.di

import fr.zesit.admin.apns.apnsEnvironment
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice

/**
 * Env iOS : spécialise le commun avec la plateforme, la version du système et
 * le label d'environnement APNs (sandbox/prod). Le token TI se fournit via la
 * variable d'environnement `TI_API_TOKEN` (process/schéma de test).
 */
actual fun platformEnv(): AppEnv = commonEnv().copy(
    platform = "ios",
    trafficToken = tiToken(),
    apnsLabel = apnsEnvironment(),
    deviceInfo = "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}",
)

private fun tiToken(): String? {
    val value = NSProcessInfo.processInfo.environment["TI_API_TOKEN"] ?: return null
    return (value as? String)?.takeIf { it.isNotBlank() }
}
