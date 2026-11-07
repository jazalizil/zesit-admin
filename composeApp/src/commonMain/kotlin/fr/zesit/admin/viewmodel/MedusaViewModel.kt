package fr.zesit.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.zesit.admin.di.AppContainer
import fr.zesit.admin.di.KpiSession
import fr.zesit.admin.kpi.KpiInputs
import fr.zesit.admin.kpi.MedusaComparison
import fr.zesit.admin.kpi.MedusaMapping
import fr.zesit.admin.kpi.MedusaStatus
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Wrapper from/to (ISO YYYY-MM-DD). */
data class MedusaPeriod(val from: String, val to: String)

data class MedusaUiState(
    val configured: Boolean = false,
    val connected: Boolean = false,
    /** Erreur de session KPI (non connecté). */
    val error: String? = null,
    val from: String,
    val to: String,
    /** Preset actif : "7" / "30" / "90" / "month" (null = période manuelle). */
    val preset: String? = "30",
    val comparison: MedusaComparison? = null,
    val mapping: MedusaMapping? = null,
    val status: MedusaStatus? = null,
    /** Comparaison en cours de chargement. */
    val loading: Boolean = false,
    /** Erreur de chargement de la comparaison. */
    val comparisonError: String? = null,
    val syncing: Boolean = false,
    val notice: String? = null,
)

/**
 * Réel vs prévisionnel — portage du store medusa (zesit-kpi/apps/web/src/store/medusa.ts) :
 * comparaison rechargée (débouncée) à chaque changement d'hypothèses du modèle chargé
 * (via [inputsFlow]) et à chaque changement de période ; synchro manuelle + mapping.
 */
@OptIn(FlowPreview::class, kotlin.time.ExperimentalTime::class)
class MedusaViewModel(
    private val container: AppContainer,
    private val inputsFlow: StateFlow<KpiInputs?>,
) : ViewModel() {

    private val api get() = container.kpiApi
    private val session: KpiSession = container.kpiSession

    private val _uiState = MutableStateFlow(MedusaUiState(from = iso(daysAgo(29)), to = iso(today())))
    val uiState: StateFlow<MedusaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            session.state.collectLatest { st ->
                _uiState.value = _uiState.value.copy(
                    configured = st.configured,
                    connected = st.connected,
                    error = if (st.connected) null else st.error,
                )
                if (st.connected) {
                    loadStatus()
                    loadMapping()
                }
            }
        }
        viewModelScope.launch {
            // La comparaison suit le modèle chargé (inputs) et la période sélectionnée.
            combine(inputsFlow, uiState) { inputs, s -> inputs to MedusaPeriod(s.from, s.to) }
                .distinctUntilChanged()
                .debounce(COMPARISON_DEBOUNCE_MS)
                .collectLatest { (inputs, _) -> if (inputs != null) loadComparison(inputs) }
        }
    }

    // --- Période ---

    fun setPreset(preset: String) {
        val today = today()
        val from = when (preset) {
            "7" -> iso(daysAgo(6))
            "30" -> iso(daysAgo(29))
            "90" -> iso(daysAgo(89))
            "month" -> iso(LocalDate(today.year, today.month, 1))
            else -> return
        }
        _uiState.value = _uiState.value.copy(preset = preset, from = from, to = iso(today))
    }

    fun setPeriod(from: String, to: String) {
        _uiState.value = _uiState.value.copy(preset = null, from = from, to = to)
    }

    // --- Données ---

    /** Statut des synchros (best effort). */
    suspend fun loadStatus() {
        try {
            val status = session.authed { base, token -> api.medusaStatus(base, token) }
            _uiState.value = _uiState.value.copy(status = status)
        } catch (_: Exception) {
            // le statut se rechargera à la prochaine synchro
        }
    }

    /** Catalogue variantes + canaux (best effort). */
    suspend fun loadMapping() {
        try {
            val mapping = session.authed { base, token -> api.medusaMapping(base, token) }
            _uiState.value = _uiState.value.copy(mapping = mapping)
        } catch (_: Exception) {
            // le mapping se rechargera à la prochaine synchro
        }
    }

    private suspend fun loadComparison(inputs: KpiInputs) {
        val period = _uiState.value.let { MedusaPeriod(it.from, it.to) }
        _uiState.value = _uiState.value.copy(loading = true, comparisonError = null)
        try {
            val comparison = session.authed { base, token ->
                api.medusaComparison(base, token, period.from, period.to, inputs)
            }
            _uiState.value = _uiState.value.copy(comparison = comparison, loading = false)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(loading = false, comparisonError = e.message)
        }
    }

    /** Synchro manuelle de la veille (days = 1), puis rechargements (web : syncNow). */
    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(syncing = true, notice = null)
            try {
                val result = session.authed { base, token -> api.medusaSyncDays(base, token, 1) }
                loadStatus()
                loadMapping()
                val notice = if (result.errors.isNotEmpty()) {
                    "Synchro en échec : " + result.errors.joinToString(" ; ") { it.error }
                } else {
                    "Synchro effectuée (${result.results.firstOrNull()?.orders_count ?: 0} commande(s) la veille)."
                }
                _uiState.value = _uiState.value.copy(syncing = false, notice = notice)
                inputsFlow.value?.let { loadComparison(it) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(syncing = false, notice = e.message ?: "Synchro impossible.")
            }
        }
    }

    /** Mappe une variante à un format (ou null pour démapper), puis recharge. */
    fun saveVariantFormat(variantId: String, formatCl: Int?) {
        viewModelScope.launch {
            try {
                session.authed { base, token -> api.setMedusaVariantFormat(base, token, variantId, formatCl) }
                loadMapping()
                inputsFlow.value?.let { loadComparison(it) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(notice = e.message ?: "Mapping non enregistré.")
            }
        }
    }

    /** Reconnexion explicite (après logout ou erreur d'identifiants). */
    fun reconnect() = session.reconnect()

    fun clearNotice() {
        _uiState.value = _uiState.value.copy(notice = null)
    }

    // --- Dates ---

    private fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    private fun daysAgo(days: Int): LocalDate = today().minus(days, DateTimeUnit.DAY)

    private fun iso(d: LocalDate): String = d.toString()

    private companion object {
        const val COMPARISON_DEBOUNCE_MS: Long = 400
    }
}
