package fr.zesit.admin.kpi

/**
 * Client de l'API KPI Flask (zesit-kpi/apps/api — https://kpi.zesit.fr/api).
 *
 * Authentification : `POST /api/login` pose un cookie JWT `zesit_token`
 * (HttpOnly, 7 jours). Ici le token est relu du header Set-Cookie et rejoué
 * manuellement via `Cookie: zesit_token=<jwt>` — pas de gestion de cookies par
 * le moteur HTTP, le token est persisté par [KpiSessionStore].
 */
interface KpiApi {

    /** Connexion : retourne le JWT (`zesit_token`). */
    suspend fun login(baseUrl: String, username: String, password: String): String

    /** Utilisateur courant — 401 si le token est absent/expiré. */
    suspend fun me(baseUrl: String, token: String): String

    /** Hypothèses par défaut (`DEFAULTS` du moteur de calcul, source de vérité). */
    suspend fun defaults(baseUrl: String, token: String): KpiInputs

    /** Calcule le prévisionnel complet + sensibilités. */
    suspend fun model(baseUrl: String, token: String, inputs: KpiInputs): KpiResponse

    /** Liste des modèles enregistrés (résumés + KPIs). */
    suspend fun models(baseUrl: String, token: String): List<KpiModelSummary>

    /** Détail d'un modèle (hypothèses + résultats complets). */
    suspend fun modelDetail(baseUrl: String, token: String, id: String): KpiModelDetail

    /** Enregistre un nouveau modèle → calcule et stocke les résultats. */
    suspend fun createModel(
        baseUrl: String,
        token: String,
        name: String,
        description: String,
        inputs: KpiInputs,
    ): KpiModelDetail

    /** Met à jour nom/description ; si `inputs != null`, recalcule les résultats. */
    suspend fun updateModel(
        baseUrl: String,
        token: String,
        id: String,
        name: String?,
        description: String?,
        inputs: KpiInputs?,
    ): KpiModelDetail

    suspend fun deleteModel(baseUrl: String, token: String, id: String)

    /** Ferme la session serveur (supprime le cookie) — best effort. */
    suspend fun logout(baseUrl: String, token: String)

    // --- Réel vs prévisionnel (ventes Medusa synchronisées côté serveur) ---

    /** Statut des dernières synchros + totaux en base. */
    suspend fun medusaStatus(baseUrl: String, token: String): MedusaStatus

    /** Synchro manuelle des `days` derniers jours écoulés (1 = la veille). */
    suspend fun medusaSyncDays(baseUrl: String, token: String, days: Int): MedusaSyncResult

    /** Catalogue variantes + sales channels (mapping format_cl éditable). */
    suspend fun medusaMapping(baseUrl: String, token: String): MedusaMapping

    /** Mappe une variante à un format (25 / 33 / 50 cl), ou null pour démapper. */
    suspend fun setMedusaVariantFormat(
        baseUrl: String,
        token: String,
        variantId: String,
        formatCl: Int?,
    ): MedusaVariant

    /** Réel vs prévisionnel sur la période [from, to] (ISO YYYY-MM-DD), selon `inputs`. */
    suspend fun medusaComparison(
        baseUrl: String,
        token: String,
        from: String,
        to: String,
        inputs: KpiInputs,
    ): MedusaComparison
}

class KpiAuthException(message: String = "Session KPI invalide ou expirée (401).") : Exception(message)
class KpiApiException(message: String) : Exception(message)

/**
 * Factory publique : construit l'implémentation Ktor sans exposer les types Ktor
 * dans l'API publique (comme [fr.zesit.admin.ntfy.createNtfyApi]).
 */
fun createKpiApi(): KpiApi = KtorKpiApi(fr.zesit.admin.ntfy.createHttpClient())
