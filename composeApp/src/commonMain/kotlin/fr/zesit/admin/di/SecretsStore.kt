package fr.zesit.admin.di

import com.russhwolf.settings.Settings

/**
 * Secrets de l'app — stockés dans le stockage **sécurisé** de la plateforme
 * (Keychain iOS / EncryptedSharedPreferences Android / Preferences desktop),
 * distincts des réglages utilisateur : le token TI fourni par l'environnement
 * ([AppEnv.trafficToken]) y est installé au démarrage s'il n'y est pas déjà.
 *
 * Le token saisi par l'utilisateur reste dans [SettingsRepository]
 * (`trafficToken`, même stockage sécurisé) et prime sur le secret installé.
 */
class SecretsStore(private val settings: Settings) {

    /** Token TI (TI_API_TOKEN) installé depuis l'env plateforme, ou saisi manuellement. */
    fun trafficApiToken(): String? = settings.getStringOrNull(KEY_TI)?.takeIf { it.isNotBlank() }

    fun saveTrafficApiToken(token: String?) {
        if (token.isNullOrBlank()) {
            settings.remove(KEY_TI)
        } else {
            settings.putString(KEY_TI, token)
        }
    }

    private companion object {
        const val KEY_TI = "ti_api_token"
    }
}
