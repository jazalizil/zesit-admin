package fr.zesit.admin.di

/**
 * Environnement d'exécution de l'app : valeurs **communes à toutes les
 * plateformes** ([commonEnv]) spécialisées par plateforme via
 * [platformEnv] (expect/actual, même pattern que [createSecureSettings]).
 *
 * Usage : `AppContainer.env` — placeholders de connexion des panneaux, ligne
 * Version des Réglages, comportement debug, fiche plateforme (APNs…).
 *
 * @param platform "desktop" | "android" | "ios" (le commun vaut "common")
 * @param debug true en build de développement (BuildConfig.DEBUG sur Android,
 *   ZESIT_DEBUG=1 sur desktop)
 * @param appName nom affiché
 * @param version version applicative (source unique : packageVersion des
 *   build.gradle des plateformes)
 * @param kpiBaseUrlDefault URL par défaut du serveur KPI (placeholder du
 *   panneau de connexion)
 * @param ntfyBaseUrlDefault URL par défaut du serveur ntfy
 * @param trafficBaseUrlDefault URL par défaut de traffic-inspector (/ti-api)
 * @param trafficToken token TI (TI_API_TOKEN du VPS) fourni par la plateforme
 *   (variable d'env TI_API_TOKEN sur desktop/iOS, propriété Gradle TI_API_TOKEN
 *   sur Android) — installé dans les secrets de l'app par AppContainer si absent
 * @param securityBaseUrlDefault URL par défaut de security-scanner (/sc-api)
 * @param securityToken token SC (SC_API_TOKEN du VPS), même mécanique que
 *   [trafficToken] — installé dans les secrets de l'app par AppContainer
 * @param ntfyToken token ntfy (NTFY_TOKEN, bearer `tk_…`), même mécanique que
 *   [trafficToken] — installé dans les secrets de l'app par AppContainer
 * @param apnsLabel environnement APNs sur iOS ("sandbox"/"production"), null ailleurs
 * @param deviceInfo fiche technique de l'appareil/hôte (OS, modèle, runtime)
 */
data class AppEnv(
    val platform: String,
    val debug: Boolean = false,
    val appName: String = "Zesit Admin",
    val version: String = "1.0.0",
    val kpiBaseUrlDefault: String = "https://kpi.zesit.fr",
    val ntfyBaseUrlDefault: String = "https://ntfy.zesit.fr",
    val trafficBaseUrlDefault: String = "https://traffic.zesit.fr",
    val trafficToken: String? = null,
    val securityBaseUrlDefault: String = "https://security.zesit.fr",
    val securityToken: String? = null,
    val ntfyToken: String? = null,
    val apnsLabel: String? = null,
    val deviceInfo: String? = null,
)

/**
 * Env commun à toutes les plateformes — une plateforme le spécialise via
 * `platformEnv().copy(...)` : tout champ ajouté ici est donc disponible
 * partout, avec surcharge possible par plateforme.
 */
fun commonEnv(): AppEnv = AppEnv(platform = "common")

/** Env effectif de la plateforme : fusion du commun + spécificités de l'actual. */
expect fun platformEnv(): AppEnv

/**
 * Surcharge d'URL d'environnement (KPI/NTFY/TRAFFIC_BASE_URL) : valable
 * uniquement si complète (http(s)://…) — une valeur tronquée ou vide retombe
 * sur le défaut du code (les fichiers d'env peuvent être vides ou mal
 * renseignés).
 */
internal fun envBaseUrl(raw: String?): String? =
    raw?.trim()?.takeUnless { it.isBlank() }?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
