package com.zesit.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zesit.admin.apns.ApnsRegistrar
import com.zesit.admin.data.AppSettings
import com.zesit.admin.di.AppContainer
import com.zesit.admin.model.Notification
import com.zesit.admin.ntfy.NtfyApi
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
            lastError = null,
            apnsStatus = null,
        )
        if (settings.isConfigured && previous != settings) {
            collectorJob?.cancel()
            collectorJob = viewModelScope.launch {
                syncThenListen(settings)
            }
        }
    }

    /** Poll initial (historique) puis WebSocket temps réel. */
    private suspend fun syncThenListen(settings: AppSettings) {
        try {
            val history = api.poll(settings.baseUrl, settings.topic, settings.token, sinceId = null)
            container.notificationRepository.addAll(history)
            _uiState.value = _uiState.value.copy(lastError = null)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(lastError = e.message)
        }
        api.connect(settings.baseUrl, settings.topic, settings.token).collect { msg ->
            container.notificationRepository.addAll(listOf(msg))
            _uiState.value = _uiState.value.copy(connected = true, lastError = null)
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
                    settings.token,
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

    /** Test de connexion (auth ntfy) depuis l'écran réglages. */
    fun testConnection(settings: AppSettings, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val ok = api.checkAccess(settings.baseUrl, settings.topic, settings.token)
                onResult(ok, if (ok) null else "HTTP non 2xx — topic inaccessible")
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    /**
     * Appelé par l'AppDelegate iOS (via [com.zesit.admin.apns.ApnsBridge]) quand APNs
     * renvoie le device token : on l'enregistre auprès du serveur ntfy (POST /v1/apns)
     * pour activer le push natif iOS.
     */
    fun onApnsDeviceToken(tokenHex: String) {
        val settings = currentSettings ?: return
        viewModelScope.launch {
            try {
                ApnsRegistrar(api, settings).register(tokenHex)
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

    private fun apnsEnvLabel(): String = com.zesit.admin.apns.apnsEnvironment()

    fun clearNotifications() {
        container.notificationRepository.clear()
    }

    override fun onCleared() {
        collectorJob?.cancel()
        api.close()
        super.onCleared()
    }
}
