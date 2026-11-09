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
