package fr.zesit.admin.model

import kotlinx.serialization.Serializable

/**
 * Message JSON tel que diffusé par le serveur ntfy (WebSocket `/ws` et flux `/json`).
 * Doc : https://docs.ntfy.sh/subscribe/api/
 */
@Serializable
data class NtfyMessage(
    val id: String,
    /** Epoch seconds */
    val time: Long,
    /** "message" pour une notification, sinon keepalive/open/... */
    val event: String? = null,
    val topic: String? = null,
    val title: String? = null,
    val message: String? = null,
    /** 1=min … 5=max (3=default) */
    val priority: Int = 3,
    val tags: List<String> = emptyList(),
    val click: String? = null,
    val icon: String? = null,
) {
    val isNotification: Boolean
        get() = event == null || event == "message"

    fun toNotification(): Notification = Notification(
        id = id,
        time = time,
        title = title?.takeIf { it.isNotBlank() } ?: "Notification",
        message = message.orEmpty(),
        priority = priority,
        tags = tags,
        click = click,
    )
}

/** Notification persistée localement (cache de l'historique du topic). */
@Serializable
data class Notification(
    val id: String,
    /** Epoch seconds */
    val time: Long,
    val title: String,
    val message: String,
    val priority: Int = 3,
    val tags: List<String> = emptyList(),
    val click: String? = null,
)
