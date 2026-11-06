package com.zesit.admin.di

import com.russhwolf.settings.Settings
import com.zesit.admin.data.NotificationRepository
import com.zesit.admin.data.SettingsRepository
import com.zesit.admin.ntfy.KtorNtfyApi
import com.zesit.admin.ntfy.NtfyApi
import com.zesit.admin.ntfy.createHttpClient

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
