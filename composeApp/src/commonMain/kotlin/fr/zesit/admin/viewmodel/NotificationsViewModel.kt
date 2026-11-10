package fr.zesit.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.zesit.admin.apns.ApnsRegistrar
import fr.zesit.admin.data.AppSettings
import fr.zesit.admin.data.ThemeMode
import fr.zesit.admin.di.AppContainer
import fr.zesit.admin.model.Notification
import fr.zesit.admin.ntfy.NtfyApi
import fr.zesit.admin.ntfy.NtfyEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val configured: Boolean = false,
    val connected: Boolean = false,
    val lastError: String? = null,
    val refreshing: Boolean = false,
    val apnsStatus: String? = null, // état de l'enregistrement APNs (iOS)
)

class NotificationsViewModel(private val container: AppContainer) : ViewModel() {

    private val api: NtfyApi get() = container.api

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    val settings: StateFlow<AppSettings> get() = container.settingsRepository.appSettings

    /** Préférence d'apparence (écran Réglages) — appliquée à la racine de l'app. */
    val themeMode: StateFlow<ThemeMode> get() = container.themeRepository.themeMode

    /** Environnement d'exécution (plateforme, valeurs par défaut communes). */
    val env get() = container.env

    private var collectorJob: Job? = null

    init {
        // Réagit aux changements de réglages (écran Réglages)
        viewModelScope.launch {
            container.settingsRepository.appSettings.collectLatest { settings ->
                restartIfNeeded(settings)
            }
        }
        viewModelScope.launch {
            container.notificationRepository.notifications.collectLatest { list ->
                _uiState.value = _uiState.value.copy(notifications = list)
            }
        }
    }

    private var currentSettings: AppSettings? = null

    private fun restartIfNeeded(settings: AppSettings) {
        val previous = currentSettings
        currentSettings = settings
        _uiState.value = _uiState.value.copy(
            configured = settings.isConfigured,
            connected = false, // la nouvelle WS n'est pas encore établie
            lastError = null,
            apnsStatus = null,
        )
        if (settings.isConfigured && previous != settings) {
            collectorJob?.cancel()
            collectorJob = viewModelScope.launch {
                syncThenListen(settings)
            }
        } else if (!settings.isConfigured) {
            // Déconnexion ntfy (ou réinitialisation) : stoppe l'écoute du topic précédent.
            collectorJob?.cancel()
            collectorJob = null
        }
    }

    /** Poll initial (historique) puis WebSocket temps réel. */
    private suspend fun syncThenListen(settings: AppSettings) {
        try {
            val history = api.poll(settings.baseUrl, settings.topic, container.secrets.ntfyAccessToken(), sinceId = null)
            container.notificationRepository.addAll(history)
            _uiState.value = _uiState.value.copy(lastError = null)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(lastError = e.message)
        }
        api.connect(settings.baseUrl, settings.topic, container.secrets.ntfyAccessToken()).collect { event ->
            when (event) {
                NtfyEvent.Connected ->
                    _uiState.value = _uiState.value.copy(connected = true)

                // La reconnexion (backoff) est déjà engagée par l'API ; l'UI affiche
                // l'état réel au lieu d'un « Connecté » périmé.
                is NtfyEvent.Disconnected ->
                    _uiState.value = _uiState.value.copy(connected = false)

                is NtfyEvent.Message -> {
                    container.notificationRepository.addAll(listOf(event.message))
                    _uiState.value = _uiState.value.copy(connected = true, lastError = null)
                }
            }
        }
    }

    fun refresh() {
        val settings = currentSettings ?: return
        if (!settings.isConfigured) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(refreshing = true)
            try {
                val missed = api.poll(
                    settings.baseUrl,
                    settings.topic,
                    container.secrets.ntfyAccessToken(),
                    sinceId = container.notificationRepository.lastMessageId,
                )
                container.notificationRepository.addAll(missed)
                _uiState.value = _uiState.value.copy(lastError = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(lastError = e.message)
            } finally {
                _uiState.value = _uiState.value.copy(refreshing = false)
            }
        }
    }

    fun saveSettings(newSettings: AppSettings) {
        container.settingsRepository.save(newSettings)
    }

    /**
     * Connexion ntfy depuis la page Notifications (état non connecté) : seul le
     * topic est saisi — l'URL vient de l'environnement, le token (NTFY_TOKEN)
     * est résolu via les secrets installés par AppContainer.
     */
    fun connectNtfy(topic: String) {
        val current = container.settingsRepository.appSettings.value
        container.settingsRepository.save(
            current.copy(
                baseUrl = container.env.ntfyBaseUrlDefault.trim().trimEnd('/'),
                topic = topic.trim(),
            ),
        )
    }

    /** true si le token ntfy est disponible (installé depuis l'environnement). */
    fun hasSecretToken(): Boolean = !container.secrets.ntfyAccessToken().isNullOrBlank()

    /** Déconnexion ntfy (bouton power de la page Notifications) : efface les réglages ntfy. */
    fun disconnectNtfy() {
        val current = container.settingsRepository.appSettings.value
        container.settingsRepository.save(current.copy(baseUrl = "", topic = ""))
    }

    /** Change le mode d'apparence (écran Réglages) : Système / Clair / Sombre. */
    fun setTheme(mode: ThemeMode) {
        container.themeRepository.set(mode)
    }

    /**
     * Réinitialisation des données (écran Réglages) : efface credentials ntfy
     * + KPI, la session JWT, l'historique des notifications et les hypothèses
     * KPI. L'apparence et les autres préférences sont conservées. Les écrans
     * réagissent via les StateFlow (retour aux panneaux de connexion).
     */
    fun resetData() {
        container.settingsRepository.save(AppSettings())
        container.kpiSessionStore.set(null)
        container.kpiHypothesesStore.clear()
        container.notificationRepository.clear()
    }

    /** Test de connexion (auth ntfy) depuis l'écran réglages. */
    fun testConnection(settings: AppSettings, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val ok = api.checkAccess(settings.baseUrl, settings.topic, container.secrets.ntfyAccessToken())
                onResult(ok, if (ok) null else "HTTP non 2xx — topic inaccessible")
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    /**
     * Appelé par l'AppDelegate iOS (via [fr.zesit.admin.apns.ApnsBridge]) quand APNs
     * renvoie le device token : on l'enregistre auprès du serveur ntfy (POST /v1/apns)
     * pour activer le push natif iOS.
     */
    fun onApnsDeviceToken(tokenHex: String) {
        val settings = currentSettings ?: return
        viewModelScope.launch {
            try {
                ApnsRegistrar(api, settings, container.secrets.ntfyAccessToken()).register(tokenHex)
                _uiState.value = _uiState.value.copy(
                    apnsStatus = "APNs enregistré (${apnsEnvLabel()}) — push natif actif",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    apnsStatus = "Échec enregistrement APNs : ${e.message}",
                )
            }
        }
    }

    private fun apnsEnvLabel(): String = fr.zesit.admin.apns.apnsEnvironment()

    fun clearNotifications() {
        container.notificationRepository.clear()
    }

    override fun onCleared() {
        collectorJob?.cancel()
        api.close()
        super.onCleared()
    }
}
