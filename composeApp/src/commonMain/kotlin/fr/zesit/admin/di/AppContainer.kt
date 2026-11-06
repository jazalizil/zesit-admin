package fr.zesit.admin.di

import com.russhwolf.settings.Settings
import fr.zesit.admin.data.NotificationRepository
import fr.zesit.admin.data.SettingsRepository
import fr.zesit.admin.ntfy.KtorNtfyApi
import fr.zesit.admin.ntfy.NtfyApi
import fr.zesit.admin.ntfy.createHttpClient

/**
 * Conteneur d'injection minimal (pas de Koin/Hilt pour rester léger) :
 * un seul [NtfyApi] Ktor par plateforme, un [SettingsRepository] et un
 * [NotificationRepository] persistants.
 */
class AppContainer {
    private val settings: Settings = Settings()

    val settingsRepository = SettingsRepository(settings)
    val notificationRepository = NotificationRepository(settings)
    val api: NtfyApi = KtorNtfyApi(createHttpClient())
}
