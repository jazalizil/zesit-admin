package fr.zesit.admin.di

import fr.zesit.admin.apns.apnsEnvironment
import platform.Foundation.NSBundle
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice

/**
 * Env iOS : spécialise le commun avec la plateforme, la version du système et
 * le label d'environnement APNs (sandbox/prod).
 *
 * Les variables d'env ([withEnvOverrides], généré par ./gradlew generateEnv
 * depuis la table des rôles) sont résolues par [envValue] : environnement du
 * process prioritaire (variables du schéma de run / export), puis valeur
 * figée dans l'Info.plist par `iosApp/iosApp/Env.xcconfig` (secrets dans
 * `Env.local.xcconfig`, non commité).
 */
actual fun platformEnv(): AppEnv = commonEnv().copy(
    platform = "ios",
    apnsLabel = apnsEnvironment(),
    deviceInfo = "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}",
).withEnvOverrides(::envValue)

/**
 * Valeur effective d'une variable d'env : l'environnement du process prioritaire,
 * sinon la clé figée dans l'Info.plist (`$(VAR)` substitué par Env.xcconfig au
 * build). Vide ou littéral `$(VAR)` non résolu → ignorer (défaut du code).
 */
private fun envValue(name: String): String? {
    NSProcessInfo.processInfo.environment[name]?.let { raw ->
        (raw as? String)?.takeIf { it.isNotBlank() }?.let { return it }
    }
    val baked = NSBundle.mainBundle.objectForInfoDictionaryKey(name) as? String
    return baked?.takeIf { it.isNotBlank() && !it.startsWith("$(") }
}
