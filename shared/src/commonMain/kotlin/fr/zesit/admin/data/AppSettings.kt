package fr.zesit.admin.data

import kotlinx.serialization.Serializable

/**
 * Réglages de connexion au serveur ntfy self-hosté, persistés localement
 * (voir [SettingsRepository]).
 *
 * @param baseUrl ex. "https://ntfy.example.com" (sans slash final)
 * @param topic nom du topic ntfy
 * @param token token d'accès ntfy (`tk_…`), null si le topic est public
 */
@Serializable
data class AppSettings(
    val baseUrl: String = "",
    val topic: String = "",
    val token: String? = null,
) {
    /** true si baseUrl + topic sont renseignés (le token reste optionnel). */
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && topic.isNotBlank()

    /** baseUrl normalisée pour les appels API (sans slash final). */
    val normalizedBaseUrl: String
        get() = baseUrl.trim().trimEnd('/')
}
