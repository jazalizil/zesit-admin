package fr.zesit.admin.kpi

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

/** Persiste le JWT KPI (`zesit_token`) entre les lancements (multiplatform-settings). */
class KpiSessionStore(private val settings: Settings) {

    fun get(): String? = settings.getStringOrNull(KEY)?.takeIf { it.isNotBlank() }

    fun set(token: String?) {
        if (token.isNullOrBlank()) settings.remove(KEY) else settings.putString(KEY, token)
    }

    private companion object {
        const val KEY = "kpi_token_v1"
    }
}

/**
 * Persiste les dernières hypothèses KPI (équivalent du localStorage du dashboard
 * web `citronnade-dashboard-v1`) : au démarrage on les recharge au lieu des
 * valeurs par défaut serveur.
 */
class KpiHypothesesStore(private val settings: Settings) {

    private val json = Json { ignoreUnknownKeys = true }

    fun load(): KpiInputs? {
        val raw = settings.getStringOrNull(KEY) ?: return null
        return runCatching { json.decodeFromString<KpiInputs>(raw) }.getOrNull()
    }

    fun save(inputs: KpiInputs) {
        settings.putString(KEY, json.encodeToString(KpiInputs.serializer(), inputs))
    }

    fun clear() {
        settings.remove(KEY)
    }

    private companion object {
        const val KEY = "kpi_inputs_v1"
    }
}
