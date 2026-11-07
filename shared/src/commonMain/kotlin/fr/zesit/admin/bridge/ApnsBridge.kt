package fr.zesit.admin.bridge

/**
 * Pont exposé au code Swift (iOS) : l'AppDelegate appelle
 * `ApnsBridge.shared.registerDeviceToken(tokenHex:)` dès qu'APNs renvoie le
 * device token.
 *
 * NB : pas de type fonction `(String) -> Unit` dans l'API publique — le générateur
 * de header ObjC de Kotlin/Native exclut alors silencieusement toute la classe.
 * On passe donc par une interface [ApnsTokenHandler] (exportée en protocole ObjC).
 */
fun interface ApnsTokenHandler {
    fun onToken(tokenHex: String)
}

object ApnsBridge {
    private var handler: ApnsTokenHandler? = null

    /** Posé par la couche UI (Compose) au démarrage de l'app iOS. */
    fun setTokenHandler(handler: ApnsTokenHandler) {
        this.handler = handler
    }

    fun clearTokenHandler() {
        handler = null
    }

    fun registerDeviceToken(tokenHex: String) {
        handler?.onToken(tokenHex)
    }
}
