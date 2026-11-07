package fr.zesit.admin.di

import com.russhwolf.settings.Settings
import fr.zesit.admin.bridge.ApnsBridge
import fr.zesit.admin.data.NotificationRepository
import fr.zesit.admin.data.SettingsRepository
import fr.zesit.admin.kpi.KpiApi
import fr.zesit.admin.kpi.KpiHypothesesStore
import fr.zesit.admin.kpi.KpiSessionStore
import fr.zesit.admin.kpi.createKpiApi
import fr.zesit.admin.ntfy.NtfyApi
import fr.zesit.admin.ntfy.createNtfyApi

/**
 * Conteneur d'injection minimal (pas de Koin/Hilt pour rester léger) :
 * un seul [NtfyApi] Ktor par plateforme, un [SettingsRepository] et un
 * [NotificationRepository] persistants.
 */
class AppContainer {
    private val settings: Settings = Settings()

    val settingsRepository = SettingsRepository(settings)
    val notificationRepository = NotificationRepository(settings)
    val api: NtfyApi = createNtfyApi()

    // API du dashboard KPI (prévisionnel citronnade) + persistance session/hypothèses
    val kpiApi: KpiApi = createKpiApi()
    val kpiSessionStore = KpiSessionStore(settings)
    val kpiHypothesesStore = KpiHypothesesStore(settings)

    // Référence explicite : rend ApnsBridge atteignable depuis l'API publique non-Compose,
    // donc exporté dans le header ObjC de ComposeApp.framework (appelé depuis Swift).
    val apnsBridge: ApnsBridge get() = ApnsBridge
}
