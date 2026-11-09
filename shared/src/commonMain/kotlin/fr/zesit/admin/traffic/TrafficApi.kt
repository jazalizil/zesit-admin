package fr.zesit.admin.traffic

/**
 * Client de l'API JSON traffic-inspector (repo `zesit-vps/traffic-inspector`,
 * servie sur `https://traffic.$ZESIT_DOMAIN/ti-api` — VPN-only via Caddy/WireGuard).
 *
 * Authentification : `Authorization: Bearer <token>` (token statique
 * `TI_API_TOKEN` défini côté VPS dans `zesit-vps/.env` ; **vide = API désactivée
 * côté serveur (404)** → [TrafficApiDisabledException]).
 *
 * Modèles miroirs des réponses JSON (clés `snake_case` exactes, voir
 * `traffic-inspector/README.md`). Non câblé dans la navigation pour l'instant :
 * à brancher dans `AppContainer` + Settings lors de l'ajout de l'écran
 * Dashboard Traffic (pattern de [fr.zesit.admin.kpi.KpiApi]).
 */
interface TrafficApi {

    /** État du service (endpoint public) — [TrafficHealth.apiEnabled] = TI_API_TOKEN configuré. */
    suspend fun health(baseUrl: String): TrafficHealth

    /** Valide le token : 200 = OK ; 404 = API désactivée ; 401 = token invalide. */
    suspend fun checkAuth(baseUrl: String, token: String)

    /** KPIs, tops (IPs/chemins/UAs/hôtes/méthodes), timeline, pays et ASN. `hours <= 0` = tout. */
    suspend fun stats(baseUrl: String, token: String, hours: Double = 24.0): TrafficStatsEnvelope

    /** Journal paginé et filtrable (tri id décroissant). */
    suspend fun requests(baseUrl: String, token: String, query: TrafficQuery = TrafficQuery()): TrafficRequestList

    /** Détail brut d'une requête (headers parsés + body stocké) — `null` si l'id n'existe pas. */
    suspend fun request(baseUrl: String, token: String, id: Long): TrafficRequestDetail?

    /** Fiche IP : enrichissement (géo, ASN, PTR, RDAP/abuse) + ses requêtes les plus récentes. */
    suspend fun ip(baseUrl: String, token: String, ip: String, limit: Int = 300): TrafficIpSheet

    /** Requêtes suspectes uniquement (scanners, injections, sondes de fichiers). */
    suspend fun alerts(baseUrl: String, token: String, hours: Double = 24.0, limit: Int = 100): TrafficAlerts
}

/** Filtres du journal ([TrafficApi.requests]). `hours <= 0` = aucune fenêtre temporelle. */
data class TrafficQuery(
    val hours: Double = 24.0,
    val limit: Int = 200,
    val offset: Int = 0,
    val ip: String? = null,
    val method: String? = null,
    /** `honeypot` (traffic.*) ou `caddy` (access logs des sites réels). */
    val source: String? = null,
    val suspicious: Int? = null,
    val status: Int? = null,
    /** Recherche plein-texte (path, query, UA, hôte, IP). */
    val search: String? = null,
)

/** Token refusé par le serveur (401). */
class TrafficAuthException(message: String = "Token traffic-inspector invalide (401).") : Exception(message)

/** `/ti-api` répond 404 : `TI_API_TOKEN` vide côté VPS, API non activée. */
class TrafficApiDisabledException(
    message: String = "API /ti-api désactivée côté serveur (TI_API_TOKEN vide).",
) : Exception(message)

/** Autre erreur HTTP du serveur (5xx, etc.). */
class TrafficApiException(message: String) : Exception(message)

/** Factory publique (le client Ktor reste interne, pattern de [fr.zesit.admin.kpi.createKpiApi]). */
fun createTrafficApi(): TrafficApi = KtorTrafficApi(fr.zesit.admin.ntfy.createHttpClient())
