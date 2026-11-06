package fr.zesit.admin.ntfy

import fr.zesit.admin.model.NtfyMessage
import kotlinx.coroutines.flow.Flow

/**
 * Client de l'API ntfy self-hosté (https://ntfy.$ZESIT_DOMAIN).
 *
 * Authentification : si le serveur a `auth-default-access: deny-all` (voir
 * ntfy/server.yml), toutes les requêtes partent avec `Authorization: Bearer <token>`
 * (token ntfy `tk_…`, provisionné via `ntfy token add <user>`).
 */
interface NtfyApi {

    /**
     * Flux temps réel des notifications du topic via WebSocket (`wss://<base>/<topic>/ws`).
     * Reconnexion automatique avec backoff. Ne retourne que les vraies notifications
     * (filtre les événements keepalive/open).
     */
    fun connect(baseUrl: String, topic: String, token: String?): Flow<NtfyMessage>

    /**
     * Poll de l'historique : `GET /<topic>/json?poll=1[&since=<id>]` (flux NDJSON).
     * `sinceId = null` → tout l'historique caché (cache serveur, 12h par défaut).
     */
    suspend fun poll(baseUrl: String, topic: String, token: String?, sinceId: String? = null): List<NtfyMessage>

    /**
     * Enregistre le device APNs (iOS) auprès du serveur : `POST /v1/apns`
     * body `{"token": "<hex>", "environment": "production"|"sandbox", "topics": [...]}`.
     * C'est ce qui permet au serveur (clé .p8 APNs configurée côté serveur, bloc
     * `apns-*` de ntfy/server.yml) de pousser nativement vers cette app.
     */
    suspend fun registerApnsDevice(
        baseUrl: String,
        token: String?,
        apnsDeviceToken: String,
        environment: String,
        topics: List<String>,
    )

    /** Sonde le topic (auth + accès) : true si `GET /<topic>/json?poll=1` répond 2xx. */
    suspend fun checkAccess(baseUrl: String, topic: String, token: String?): Boolean

    fun close()
}
