package fr.zesit.admin.data

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Persiste les [AppSettings] via multiplatform-settings (Keychain iOS,
 * EncryptedSharedPreferences Android). Expose un [StateFlow] pour que l'UI
 * réagisse aux changements (ex. écran Réglages → reconnecte le WebSocket).
 */
class SettingsRepository(private val settings: Settings) {

    private val json = Json { ignoreUnknownKeys = true }

    private val _appSettings = MutableStateFlow(load())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    fun save(newSettings: AppSettings) {
        settings.putString(KEY, json.encodeToString(newSettings))
        _appSettings.value = newSettings
    }

    private fun load(): AppSettings {
        val raw = settings.getStringOrNull(KEY) ?: return AppSettings()
        return runCatching { json.decodeFromString<AppSettings>(raw) }.getOrDefault(AppSettings())
    }

    private companion object {
        const val KEY = "app_settings"
    }
}
