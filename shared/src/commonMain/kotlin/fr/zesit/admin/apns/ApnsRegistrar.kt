package fr.zesit.admin.apns

import fr.zesit.admin.data.AppSettings
import fr.zesit.admin.ntfy.NtfyApi

/**
 * Enregistre le device APNs d'iOS auprès du serveur ntfy self-hosté
 * (`POST /v1/apns`, voir ntfy/server.yml bloc `apns-*` côté VPS) pour recevoir
 * les notifications push natives. Sans cet enregistrement, l'app ne reçoit
 * les notifs que quand elle est ouverte (WebSocket) ou en polling.
 */
class ApnsRegistrar(
    private val api: NtfyApi,
    private val settings: AppSettings,
) {
    suspend fun register(deviceTokenHex: String) {
        if (!settings.isConfigured) return
        api.registerApnsDevice(
            baseUrl = settings.baseUrl,
            token = settings.token,
            apnsDeviceToken = deviceTokenHex,
            environment = apnsEnvironment(),
            topics = listOf(settings.topic),
        )
    }
}
