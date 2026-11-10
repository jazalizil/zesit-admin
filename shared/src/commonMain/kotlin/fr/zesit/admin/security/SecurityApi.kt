package fr.zesit.admin.security

/**
 * Client de l'API JSON security-scanner (repo `zesit-vps/security-scanner`,
 * servie sur `https://security.$ZESIT_DOMAIN/sc-api` — VPN-only via
 * Caddy/WireGuard).
 *
 * Authentification : `Authorization: Bearer <token>` (token statique
 * `SC_API_TOKEN` défini côté VPS dans `zesit-vps/.env` ; **vide = API
 * désactivée côté serveur (404)** → [SecurityApiDisabledException]).
 *
 * Modèles miroirs des réponses JSON (clés `snake_case` exactes, voir
 * `security-scanner/README.md`). Pattern de [fr.zesit.admin.traffic.TrafficApi].
 */
interface SecurityApi {

    /** État du service (endpoint public) — `api_enabled` = SC_API_TOKEN configuré. */
    suspend fun health(baseUrl: String): SecurityHealth

    /** État courant : totaux par sévérité + une fiche par image (deltas inclus). */
    suspend fun summary(baseUrl: String, token: String): SecuritySummary

    /** Vulnérabilités (état courant) d'une image, filtrables. */
    suspend fun findings(
        baseUrl: String,
        token: String,
        image: String,
        severity: String? = null,
        q: String? = null,
        limit: Int = 100,
        offset: Int = 0,
    ): SecurityFindingsList

    /** Historique des cycles de scan. */
    suspend fun scans(baseUrl: String, token: String, limit: Int = 20, offset: Int = 0): SecurityScans

    /** Déclenche un scan manuel : id du run créé ; [SecurityScanConflictException] si déjà en cours. */
    suspend fun triggerScan(baseUrl: String, token: String): Long
}

/** Token refusé par le serveur (401). */
class SecurityAuthException(message: String = "Token security-scanner invalide (401).") : Exception(message)

/** `/sc-api` répond 404 : `SC_API_TOKEN` vide côté VPS, API non activée. */
class SecurityApiDisabledException(
    message: String = "API /sc-api désactivée côté serveur (SC_API_TOKEN vide).",
) : Exception(message)

/** POST /sc-api/scan alors qu'un scan est déjà en cours/attente (409). */
class SecurityScanConflictException(
    message: String = "Un scan est déjà en cours ou en attente sur le serveur.",
) : Exception(message)

/** Autre erreur HTTP du serveur (5xx, etc.). */
class SecurityApiException(message: String) : Exception(message)

/** Factory publique (le client Ktor reste interne, pattern de createTrafficApi). */
fun createSecurityApi(): SecurityApi = KtorSecurityApi(fr.zesit.admin.ntfy.createHttpClient())
