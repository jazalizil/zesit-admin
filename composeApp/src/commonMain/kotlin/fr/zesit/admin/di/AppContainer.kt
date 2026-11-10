package fr.zesit.admin.di

import fr.zesit.admin.bridge.ApnsBridge
import fr.zesit.admin.data.NotificationRepository
import fr.zesit.admin.data.SettingsRepository
import fr.zesit.admin.data.ThemeRepository
import fr.zesit.admin.kpi.KpiApi
import fr.zesit.admin.kpi.KpiHypothesesStore
import fr.zesit.admin.kpi.KpiSessionStore
import fr.zesit.admin.kpi.createKpiApi
import fr.zesit.admin.ntfy.NtfyApi
import fr.zesit.admin.ntfy.createNtfyApi
import fr.zesit.admin.traffic.TrafficApi
import fr.zesit.admin.traffic.createTrafficApi
import fr.zesit.admin.security.SecurityApi
import fr.zesit.admin.security.createSecurityApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Conteneur d'injection minimal (pas de Koin/Hilt pour rester léger). Deux
 * stockages Settings par plateforme : `secureSettings` pour les credentials
 * (token ntfy, JWT KPI — Keychain iOS / EncryptedSharedPreferences Android),
 * `cacheSettings` pour le reste sans secret (historique, hypothèses KPI).
 */
class AppContainer {
    // Réglages (avec le token ntfy) : stockage sécurisé par plateforme.
    private val secureSettings = createSecureSettings()

    // Cache de l'historique : stockage standard (aucun secret dedans).
    private val cacheSettings = createCacheSettings()

    // Réglages (avec le token ntfy) : stockage sécurisé par plateforme,
    // migrés une fois depuis l'ancien stockage non chiffré s'il existe.
    val settingsRepository = SettingsRepository(secureSettings, legacySettings = createLegacySettings())
    val notificationRepository = NotificationRepository(cacheSettings)
    val api: NtfyApi = createNtfyApi()
    val themeRepository = ThemeRepository(cacheSettings)

    // Dashboard d'analyse du trafic (traffic-inspector /ti-api — VPN-only).
    val trafficApi: TrafficApi = createTrafficApi()

    // Scanner de vulnérabilités des conteneurs (security-scanner /sc-api — VPN-only).
    val securityApi: SecurityApi = createSecurityApi()

    // Secrets de l'app : tokens TI/SC installés depuis l'env plateforme, sur stockage sécurisé.
    val secrets = SecretsStore(secureSettings)

    // Environnement d'exécution : commun + spécialités de la plateforme. Les
    // tokens de l'env sont (ré)installés dans les secrets dès qu'ils diffèrent —
    // l'environnement prime sur un secret périmé (rotation des *_API_TOKEN / NTFY_TOKEN).
    val env: AppEnv = platformEnv().also { env ->
        if (!env.trafficToken.isNullOrBlank() && secrets.trafficApiToken() != env.trafficToken) {
            secrets.saveTrafficApiToken(env.trafficToken)
        }
        if (!env.securityToken.isNullOrBlank() && secrets.securityApiToken() != env.securityToken) {
            secrets.saveSecurityApiToken(env.securityToken)
        }
        if (!env.ntfyToken.isNullOrBlank() && secrets.ntfyAccessToken() != env.ntfyToken) {
            secrets.saveNtfyAccessToken(env.ntfyToken)
        }
    }

    // API du dashboard KPI (prévisionnel citronnade) + persistance session/hypothèses.
    // Le JWT KPI est un credential → stockage sécurisé ; les hypothèses sans
    // secret → stockage standard.
    val kpiApi: KpiApi = createKpiApi()
    val kpiSessionStore = KpiSessionStore(secureSettings)
    val kpiHypothesesStore = KpiHypothesesStore(cacheSettings)

    // Session KPI partagée (KPI + Réel/Prévisionnel) — scope applicatif
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    internal val kpiSession = KpiSession(kpiApi, kpiSessionStore, appScope, settingsRepository.appSettings)

    // Référence explicite : rend ApnsBridge atteignable depuis l'API publique non-Compose,
    // donc exporté dans le header ObjC de ComposeApp.framework (appelé depuis Swift).
    val apnsBridge: ApnsBridge get() = ApnsBridge
}
