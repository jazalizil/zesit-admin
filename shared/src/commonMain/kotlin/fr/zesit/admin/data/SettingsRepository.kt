package fr.zesit.admin.data

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Persiste les [AppSettings] via multiplatform-settings, dans le stockage fourni
 * par la plateforme d'exécution (Keychain sur iOS, EncryptedSharedPreferences sur
 * Android, Preferences Java en clair sur desktop — pas de keystore standard JVM).
 * Expose un [StateFlow] pour que l'UI réagisse aux changements (ex. écran
 * Réglages → reconnecte le WebSocket).
 */
class SettingsRepository(
    private val settings: Settings,
    /** Ancien stockage (avant chiffrement) : les réglages existants y sont migrés une fois. */
    private val legacySettings: Settings? = null,
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val _appSettings = MutableStateFlow(load())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    fun save(newSettings: AppSettings) {
        settings.putString(KEY, json.encodeToString(newSettings))
        _appSettings.value = newSettings
    }

    private fun load(): AppSettings {
        val raw = settings.getStringOrNull(KEY)
            ?: migrateFromLegacy()
            ?: return AppSettings()
        return runCatching { json.decodeFromString<AppSettings>(raw) }.getOrDefault(AppSettings())
    }

    /** Copie les réglages depuis l'ancien stockage vers le nouveau, une seule fois. */
    private fun migrateFromLegacy(): String? {
        val legacy = legacySettings ?: return null
        val legacyRaw = legacy.getStringOrNull(KEY) ?: return null
        settings.putString(KEY, legacyRaw)
        legacy.remove(KEY)
        return legacyRaw
    }

    private companion object {
        const val KEY = "app_settings"
    }
}
