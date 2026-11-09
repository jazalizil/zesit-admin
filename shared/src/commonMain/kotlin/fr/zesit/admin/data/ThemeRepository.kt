package fr.zesit.admin.data

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mode d'apparence de l'app (écran Réglages) : suit l'OS, forcé clair ou
 * forcé sombre.
 */
enum class ThemeMode(val label: String) {
    SYSTEM("Système"),
    LIGHT("Clair"),
    DARK("Sombre"),
}

/**
 * Préférence d'apparence persistée dans le stockage de cache (aucun secret)
 * et exposée en [StateFlow] : le thème s'applique immédiatement au clic.
 * Choix de cycle : Système → Clair → Sombre → Système.
 */
class ThemeRepository(private val settings: Settings) {

    private val _themeMode = MutableStateFlow(load())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun set(mode: ThemeMode) {
        settings.putString(KEY, mode.name)
        _themeMode.value = mode
    }

    private fun load(): ThemeMode =
        settings.getStringOrNull(KEY)?.let { raw ->
            runCatching { ThemeMode.valueOf(raw) }.getOrNull()
        } ?: ThemeMode.SYSTEM

    private companion object {
        const val KEY = "theme_mode"
    }
}
