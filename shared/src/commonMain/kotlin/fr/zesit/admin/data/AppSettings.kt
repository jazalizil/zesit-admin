package fr.zesit.admin.data

import kotlinx.serialization.Serializable

/**
 * Réglages de connexion au serveur ntfy self-hosté, persistés localement
 * (voir [SettingsRepository]).
 *
 * @param baseUrl ex. "https://ntfy.example.com" (sans slash final) — renseigné
 *   par l'app depuis l'environnement, plus jamais saisi
 * @param topic nom du topic ntfy (seule saisie de connexion ntfy) ; le token
 *   d'accès vient de l'environnement (NTFY_TOKEN) et vit dans les secrets de
 *   l'app, il n'est plus persisté ici
 */
@Serializable
data class AppSettings(
    val baseUrl: String = "",
    val topic: String = "",
    // Connexion au serveur KPI (dashboard prévisionnel, API Flask kpi.zesit.fr)
    val kpiBaseUrl: String = "",
    val kpiUser: String = "",
    val kpiPassword: String = "",
    // Connexion à traffic-inspector (/ti-api — VPN-only). Le token TI n'est pas
    // saisi : il vient de l'environnement et vit dans les secrets de l'app.
    val trafficBaseUrl: String = "",
    // Connexion à security-scanner (/sc-api — VPN-only, vulnérabilités des
    // conteneurs Trivy). Le token SC n'est pas saisi : même mécanique que TI.
    val securityBaseUrl: String = "",
) {
    /** true si baseUrl + topic sont renseignés. */
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && topic.isNotBlank()

    /** true si le serveur KPI + l'utilisateur sont renseignés. */
    val isKpiConfigured: Boolean
        get() = kpiBaseUrl.isNotBlank() && kpiUser.isNotBlank()

    /** baseUrl normalisée pour les appels API (sans slash final). */
    val normalizedBaseUrl: String
        get() = baseUrl.trim().trimEnd('/')

    /** baseUrl KPI normalisée pour les appels API (sans slash final). */
    val normalizedKpiBaseUrl: String
        get() = kpiBaseUrl.trim().trimEnd('/')

    /** true si l'URL de traffic-inspector est renseignée (le token reste requis côté serveur). */
    val isTrafficConfigured: Boolean
        get() = trafficBaseUrl.isNotBlank()

    /** baseUrl traffic-inspector normalisée pour les appels API (sans slash final). */
    val normalizedTrafficBaseUrl: String
        get() = trafficBaseUrl.trim().trimEnd('/')

    /** true si l'URL de security-scanner est renseignée (le token reste requis côté serveur). */
    val isSecurityConfigured: Boolean
        get() = securityBaseUrl.isNotBlank()

    /** baseUrl security-scanner normalisée pour les appels API (sans slash final). */
    val normalizedSecurityBaseUrl: String
        get() = securityBaseUrl.trim().trimEnd('/')
}
