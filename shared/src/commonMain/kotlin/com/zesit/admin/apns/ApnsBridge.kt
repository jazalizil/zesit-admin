package com.zesit.admin.apns

/**
 * Pont exposé au code Swift (iOS) : l'AppDelegate appelle
 * `ApnsBridge.shared.registerDeviceToken(tokenHex:)` dès qu'APNs renvoie le
 * device token. Le handler est posé par la couche UI (Compose) au démarrage.
 *
 * Le token est ensuite enregistré auprès du serveur ntfy (`POST /v1/apns`) pour
 * que le VPS puisse pousser nativement via APNs (clé .p8 côté serveur).
 */
object ApnsBridge {
    @kotlin.concurrent.Volatile
    var handler: ((tokenHex: String) -> Unit)? = null

    fun registerDeviceToken(tokenHex: String) {
        handler?.invoke(tokenHex)
    }
}
