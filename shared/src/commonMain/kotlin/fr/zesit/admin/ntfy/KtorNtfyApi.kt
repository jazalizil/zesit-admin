package fr.zesit.admin.ntfy

import fr.zesit.admin.model.NtfyMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Serializable
private data class ApnsDeviceRequest(
    val token: String,
    val environment: String,
    val topics: List<String>,
)

internal class KtorNtfyApi(private val client: HttpClient) : NtfyApi {

    override fun connect(baseUrl: String, topic: String, token: String?): Flow<NtfyEvent> =
        flow {
            val wsUrl = baseUrl.toWsUrl().trimEnd('/') + "/$topic/ws"
            var backoffMs = RECONNECT_DELAY_MS
            while (true) {
                val session = try {
                    client.webSocketSession {
                        url { takeFrom(wsUrl) }
                        token?.takeIf { it.isNotBlank() }
                            ?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                    }
                } catch (e: Exception) {
                    // serveur injoignable : on notifie puis retente (backoff exponentiel)
                    emit(NtfyEvent.Disconnected(e.message))
                    delay(backoffMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
                    continue
                }
                // connexion établie : on notifie et réinitialise le backoff
                backoffMs = RECONNECT_DELAY_MS
                emit(NtfyEvent.Connected)
                try {
                    while (true) {
                        val frame = session.incoming.receive()
                        if (frame is Frame.Text) {
                            val msg = runCatching { json.decodeFromString<NtfyMessage>(frame.readText()) }.getOrNull()
                            if (msg != null && msg.isNotification) emit(NtfyEvent.Message(msg))
                        }
                    }
                } catch (e: Exception) {
                    // WS fermée (réseau, serveur redémarré…) : on notifie et reboucle
                    emit(NtfyEvent.Disconnected(e.message))
                }
                try { session.cancel() } catch (_: Exception) {}
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
            }
        }

    override suspend fun poll(
        baseUrl: String,
        topic: String,
        token: String?,
        sinceId: String?,
    ): List<NtfyMessage> {
        val url = buildString {
            append(baseUrl.trimEnd('/')).append('/').append(topic).append("/json?poll=1")
            if (sinceId != null) append("&since=").append(sinceId.encodeURLParameter())
        }
        val response = client.get(url) {
            token?.takeIf { it.isNotBlank() }
                ?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
        if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
            throw NtfyAuthException(response.status.value)
        }
        if (!response.status.isSuccess()) throw NtfyException("poll HTTP ${response.status.value}")
        return response.bodyAsText()
            .lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line -> runCatching { json.decodeFromString<NtfyMessage>(line) }.getOrNull() }
            .filter { it.isNotification }
            .toList()
    }

    override suspend fun registerApnsDevice(
        baseUrl: String,
        token: String?,
        apnsDeviceToken: String,
        environment: String,
        topics: List<String>,
    ) {
        val response = client.post("${baseUrl.trimEnd('/')}/v1/apns") {
            contentType(ContentType.Application.Json)
            token?.takeIf { it.isNotBlank() }
                ?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            setBody(ApnsDeviceRequest(apnsDeviceToken.lowercase(), environment, topics))
        }
        if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
            throw NtfyAuthException(response.status.value)
        }
        if (!response.status.isSuccess()) {
            throw NtfyException("registerApnsDevice HTTP ${response.status.value} : ${response.bodyAsText()}")
        }
    }

    override suspend fun checkAccess(baseUrl: String, topic: String, token: String?): Boolean {
        val response = client.get("${baseUrl.trimEnd('/')}/$topic/json?poll=1") {
            token?.takeIf { it.isNotBlank() }
                ?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
        return response.status.isSuccess()
    }

    override fun close() {
        client.close()
    }

    private fun String.toWsUrl(): String =
        when {
            startsWith("https://") -> "wss://" + removePrefix("https://")
            startsWith("http://") -> "ws://" + removePrefix("http://")
            else -> "wss://$this"
        }

    private companion object {
        const val RECONNECT_DELAY_MS = 5_000L
        const val MAX_RECONNECT_DELAY_MS = 60_000L
    }
}

class NtfyAuthException(val status: Int) : Exception("Auth ntfy refusée (HTTP $status) — vérifier le token")
class NtfyException(message: String) : Exception(message)
