package fr.zesit.admin.di

import fr.zesit.admin.apns.apnsEnvironment
import platform.UIKit.UIDevice

/**
 * Env iOS : spécialise le commun avec la plateforme, la version du système et
 * le label d'environnement APNs (sandbox/prod) déterminé côté iOS.
 */
actual fun platformEnv(): AppEnv = commonEnv().copy(
    platform = "ios",
    apnsLabel = apnsEnvironment(),
    deviceInfo = "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}",
)
