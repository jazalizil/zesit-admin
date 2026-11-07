package fr.zesit.admin.di

import fr.zesit.admin.data.AppSettings
import fr.zesit.admin.kpi.KpiApi
import fr.zesit.admin.kpi.KpiApiException
import fr.zesit.admin.kpi.KpiAuthException
import fr.zesit.admin.kpi.KpiSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class KpiSessionState(
    val configured: Boolean = false,
    val connected: Boolean = false,
    val userName: String? = null,
    val error: String? = null,
)

/**
 * Session KPI (API Flask, cookie JWT `zesit_token`) partagée par les ViewModels KPI
 * et Réel/Prévisionnel : login automatique au changement de réglages, re-login sur 401,
 * persistance du token entre les lancements. `internal` : rien n'apparaît dans le
 * header ObjC du framework iOS.
 */
internal class KpiSession(
    private val api: KpiApi,
    private val store: KpiSessionStore,
    private val scope: CoroutineScope,
    appSettings: StateFlow<AppSettings>,
) {

    /** (baseUrl, user, password) — toute modification relance la connexion. */
    private var config: Triple<String, String, String>? = null
    private var token: String? = null
    private var sessionClosed = false

    private val _state = MutableStateFlow(KpiSessionState())
    val state: StateFlow<KpiSessionState> = _state.asStateFlow()

    init {
        scope.launch {
            appSettings.collectLatest { settings ->
                val newConfig = Triple(
                    settings.normalizedKpiBaseUrl,
                    settings.kpiUser.trim(),
                    settings.kpiPassword,
                )
                if (newConfig != config) {
                    config = newConfig
                    sessionClosed = false
                    probe()
                }
            }
        }
    }

    fun baseUrl(): String? = config?.first?.takeIf { it.isNotBlank() }

    /**
     * Exécute `block` avec un token valide : re-login automatique sur 401
     * (une seule tentative, sinon l'erreur remonte à l'UI).
     */
    suspend fun <T> authed(block: suspend (baseUrl: String, token: String) -> T): T {
        val (base, user, pass) = config ?: throw KpiApiException("Serveur KPI non configuré.")
        if (sessionClosed) throw KpiAuthException("Session KPI fermée — reconnectez-vous.")
        var current = token ?: store.get()
        if (current == null) {
            current = api.login(base, user, pass).also { store.set(it); token = it }
        }
        return try {
            block(base, current)
        } catch (_: KpiAuthException) {
            val fresh = api.login(base, user, pass).also { store.set(it); token = it }
            try {
                block(base, fresh)
            } catch (_: KpiAuthException) {
                _state.value = _state.value.copy(connected = false)
                throw KpiAuthException("Identifiants KPI invalides.")
            }
        }
    }

    fun logout() {
        scope.launch {
            val base = baseUrl()
            token?.takeIf { base != null }?.let { t -> runCatching { api.logout(base!!, t) } }
            token = null
            store.set(null)
            sessionClosed = true
            _state.value = _state.value.copy(
                connected = false,
                userName = null,
                error = "Session KPI fermée — reconnectez-vous.",
            )
        }
    }

    /** Reconnexion explicite (après logout ou erreur d'identifiants). */
    fun reconnect() {
        scope.launch {
            sessionClosed = false
            probe()
        }
    }

    /** Applique une nouvelle configuration : teste la session (`/me`) ou login. */
    private suspend fun probe() {
        val (base, user, _) = config ?: return
        if (base.isBlank() || user.isBlank()) {
            token = null
            store.set(null)
            _state.value = KpiSessionState(configured = false)
            return
        }
        _state.value = _state.value.copy(configured = true, connected = false, error = null)
        try {
            val name = authed { b, t -> api.me(b, t) }
            _state.value = _state.value.copy(connected = true, userName = name)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                connected = false,
                error = e.message ?: "Connexion KPI impossible.",
            )
        }
    }
}
