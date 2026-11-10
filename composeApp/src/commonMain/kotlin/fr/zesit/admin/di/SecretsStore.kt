package fr.zesit.admin.di

import com.russhwolf.settings.Settings

/**
 * Secrets de l'app — stockés dans le stockage **sécurisé** de la plateforme
 * (Keychain iOS / EncryptedSharedPreferences Android, Preferences desktop),
 * distincts des réglages utilisateur : les tokens fournis par l'environnement
 * ([AppEnv.trafficToken], [AppEnv.securityToken]) y sont installés au démarrage
 * s'ils diffèrent (l'environnement prime sur un secret périmé).
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

    /** Token SC (SC_API_TOKEN) de security-scanner, même mécanique que le token TI. */
    fun securityApiToken(): String? = settings.getStringOrNull(KEY_SC)?.takeIf { it.isNotBlank() }

    fun saveSecurityApiToken(token: String?) {
        if (token.isNullOrBlank()) {
            settings.remove(KEY_SC)
        } else {
            settings.putString(KEY_SC, token)
        }
    }

    /** Token ntfy (NTFY_TOKEN, bearer tk_…) installé depuis l'env plateforme. */
    fun ntfyAccessToken(): String? = settings.getStringOrNull(KEY_NTFY)?.takeIf { it.isNotBlank() }

    fun saveNtfyAccessToken(token: String?) {
        if (token.isNullOrBlank()) {
            settings.remove(KEY_NTFY)
        } else {
            settings.putString(KEY_NTFY, token)
        }
    }

    private companion object {
        const val KEY_TI = "ti_api_token"
        const val KEY_SC = "sc_api_token"
        const val KEY_NTFY = "ntfy_access_token"
    }
}
