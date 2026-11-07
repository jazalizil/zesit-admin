package fr.zesit.admin.data

import com.russhwolf.settings.Settings
import fr.zesit.admin.model.Notification
import fr.zesit.admin.model.NtfyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Cache local persistant de l'historique du topic ntfy : les notifications
 * reçues (WebSocket + poll) sont stockées ici pour être réaffichées au
 * redémarrage de l'app. Triées de la plus récente à la plus ancienne,
 * tronquées à [MAX_STORED] entrées.
 */
class NotificationRepository(private val settings: Settings) {

    private val json = Json { ignoreUnknownKeys = true }

    private val _notifications = MutableStateFlow(load())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    /** id du message le plus récent connu (pour `poll?since=<id>`). */
    val lastMessageId: String?
        get() = _notifications.value.firstOrNull()?.id

    fun addAll(messages: List<NtfyMessage>) {
        val incoming = messages.filter { it.isNotification }.map { it.toNotification() }
        if (incoming.isEmpty()) return
        val merged = (incoming + _notifications.value)
            .distinctBy { it.id }
            .sortedByDescending { it.time }
            .take(MAX_STORED)
        persist(merged)
    }

    fun clear() {
        settings.remove(KEY)
        _notifications.value = emptyList()
    }

    private fun persist(list: List<Notification>) {
        settings.putString(KEY, json.encodeToString(list))
        _notifications.value = list
    }

    private fun load(): List<Notification> {
        val raw = settings.getStringOrNull(KEY) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Notification>>(raw) }
            .getOrDefault(emptyList())
            .sortedByDescending { it.time }
    }

    private companion object {
        const val KEY = "notifications"
        const val MAX_STORED = 200
    }
}
