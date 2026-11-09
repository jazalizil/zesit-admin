package fr.zesit.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.zesit.admin.di.AppContainer
import fr.zesit.admin.traffic.TrafficApiDisabledException
import fr.zesit.admin.traffic.TrafficAuthException
import fr.zesit.admin.traffic.TrafficApiException
import fr.zesit.admin.traffic.TrafficHealth
import fr.zesit.admin.traffic.TrafficIpSheet
import fr.zesit.admin.traffic.TrafficQuery
import fr.zesit.admin.traffic.TrafficRequestList
import fr.zesit.admin.traffic.TrafficAlerts
import fr.zesit.admin.traffic.TrafficStatsEnvelope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

data class TrafficUiState(
    val configured: Boolean = false,
    /** Dernière réponse /ti-api/health (api_enabled = TI_API_TOKEN configuré). */
    val health: TrafficHealth? = null,
    /** Fenêtre d'analyse en heures : 24, 168 ou 0 (= tout). */
    val hours: Double = 24.0,
    val stats: TrafficStatsEnvelope? = null,
    val alerts: TrafficAlerts? = null,
    val journal: TrafficRequestList? = null,
    // Filtres du journal
    val search: String = "",
    /** null = toutes sources ; "honeypot" ou "caddy". */
    val source: String? = null,
    val suspiciousOnly: Boolean = false,
    val ipSheet: TrafficIpSheet? = null,
    val ipLoading: Boolean = false,
    val loading: Boolean = false,
    val journalLoading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
)

/**
 * Dashboard d'analyse du trafic web — consomme l'API JSON traffic-inspector
 * (`/ti-api`, zesit-vps/traffic-inspector, VPN-only). Réglages de connexion
 * (URL + token statique TI_API_TOKEN) édités depuis la page même : la moindre
 * modification des réglages relance le chargement complet.
 */
@OptIn(FlowPreview::class)
class TrafficViewModel(private val container: AppContainer) : ViewModel() {

    private val api get() = container.trafficApi

    private val _uiState = MutableStateFlow(TrafficUiState())
    val uiState: StateFlow<TrafficUiState> = _uiState.asStateFlow()

    /** Réglages enregistrés (pré-remplissage du panneau de connexion). */
    val settings get() = container.settingsRepository.appSettings

    /** Environnement d'exécution (plateforme, valeurs par défaut communes). */
    val env get() = container.env

    /** Token TI effectif : réglage utilisateur sinon secret installé depuis l'env. */
    fun effectiveToken(): String =
        container.settingsRepository.appSettings.value.trafficToken.ifBlank {
            container.secrets.trafficApiToken().orEmpty()
        }

    /** (baseUrl, token) courants — toute modification relance le chargement. */
    private var currentConfig: Pair<String, String>? = null

    /** L'auto-configuration ne s'applique qu'une fois par session : une Déconnexion explicite reste effective jusqu'au prochain lancement. */
    private var autoConfigAttempted = false

    /** Pipeline des filtres du journal (recherche débouncée). */
    private val journalFilters = MutableStateFlow(Triple("", null as String?, false))

    private var loadJob: Job? = null
    private var journalJob: Job? = null

    init {
        viewModelScope.launch {
            container.settingsRepository.appSettings.collectLatest { s ->
                // Auto-configuration (une fois par session) : si rien n'est saisi
                // mais que l'app dispose du token TI (env plateforme installé
                // dans les secrets), on branche l'URL par défaut de l'env — la
                // page se connecte seule.
                if (!autoConfigAttempted && !s.isTrafficConfigured) {
                    val token = container.secrets.trafficApiToken()
                    if (!token.isNullOrBlank()) {
                        autoConfigAttempted = true
                        container.settingsRepository.save(
                            s.copy(
                                trafficBaseUrl = container.env.trafficBaseUrlDefault,
                                trafficToken = token,
                            ),
                        )
                        return@collectLatest
                    }
                }
                // Token effectif : réglage utilisateur sinon secret installé
                // depuis l'env plateforme (les deux vivent en stockage sécurisé).
                val cfg = s.normalizedTrafficBaseUrl to s.trafficToken.ifBlank {
                    container.secrets.trafficApiToken().orEmpty()
                }
                if (cfg != currentConfig) {
                    currentConfig = cfg
                    if (cfg.first.isBlank()) {
                        _uiState.value = TrafficUiState()
                    } else {
                        // Configuré dès maintenant : la page affiche le chargement
                        // (et non le panneau de connexion) pendant le premier appel.
                        _uiState.value = _uiState.value.copy(configured = true)
                        reloadAll()
                    }
                }
            }
        }
        viewModelScope.launch {
            journalFilters.debounce(400).collectLatest {
                // Recharger le journal à chaque changement de filtre (si connecté).
                if (_uiState.value.stats != null) loadJournal()
            }
        }
    }

    // --- Connexion / déconnexion ---

    /** Connexion depuis la page Traffic (URL + token statique TI_API_TOKEN). */
    fun connect(baseUrl: String, token: String) {
        val current = container.settingsRepository.appSettings.value
        container.settingsRepository.save(
            current.copy(
                trafficBaseUrl = baseUrl.trim().trimEnd('/'),
                trafficToken = token.trim(),
            ),
        )
    }

    /** Déconnexion (bouton power) : efface les réglages traffic-inspector. */
    fun disconnect() {
        val current = container.settingsRepository.appSettings.value
        container.settingsRepository.save(
            current.copy(trafficBaseUrl = "", trafficToken = ""),
        )
    }

    // --- Filtres ---

    fun setHours(hours: Double) {
        if (_uiState.value.hours == hours) return
        _uiState.value = _uiState.value.copy(hours = hours)
        reloadAll()
    }

    fun setSearch(search: String) {
        _uiState.value = _uiState.value.copy(search = search)
        journalFilters.value = Triple(search, _uiState.value.source, _uiState.value.suspiciousOnly)
    }

    fun setSource(source: String?) {
        if (_uiState.value.source == source) return
        _uiState.value = _uiState.value.copy(source = source)
        journalFilters.value = Triple(_uiState.value.search, source, _uiState.value.suspiciousOnly)
    }

    fun setSuspiciousOnly(on: Boolean) {
        if (_uiState.value.suspiciousOnly == on) return
        _uiState.value = _uiState.value.copy(suspiciousOnly = on)
        journalFilters.value = Triple(_uiState.value.search, _uiState.value.source, on)
    }

    fun loadMore() {
        val journal = _uiState.value.journal ?: return
        if (_uiState.value.loadingMore) return
        val base = currentConfig ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingMore = true)
            try {
                val query = TrafficQuery(
                    hours = _uiState.value.hours,
                    limit = PAGE_SIZE,
                    offset = journal.requests.size,
                    ip = null,
                    method = null,
                    source = _uiState.value.source,
                    suspicious = if (_uiState.value.suspiciousOnly) 1 else null,
                    status = null,
                    search = _uiState.value.search.ifBlank { null },
                )
                val more = api.requests(base.first, base.second, query)
                _uiState.value = _uiState.value.copy(
                    journal = journal.copy(
                        requests = journal.requests + more.requests,
                        limit = journal.requests.size + more.requests.size,
                    ),
                    loadingMore = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loadingMore = false, error = friendly(e))
            }
        }
    }

    // --- Fiche IP ---

    fun openIp(ip: String) {
        val base = currentConfig ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ipLoading = true)
            try {
                val sheet = api.ip(base.first, base.second, ip)
                _uiState.value = _uiState.value.copy(ipSheet = sheet, ipLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(ipLoading = false, error = friendly(e))
            }
        }
    }

    fun closeIp() {
        _uiState.value = _uiState.value.copy(ipSheet = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // --- Chargements ---

    /** Recharge santé + stats + alertes ; le journal suit via les filtres. */
    fun reloadAll() {
        val base = currentConfig ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val health = api.health(base.first)
                if (!health.ok || !health.api_enabled) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = if (health.api_enabled) {
                            "traffic-inspector signale un problème (${if (health.secrets_captured) "" else "secrets non capturés"})."
                        } else {
                            "API /ti-api désactivée côté serveur (TI_API_TOKEN vide)."
                        },
                    )
                    return@launch
                }
                val hours = _uiState.value.hours
                val stats = api.stats(base.first, base.second, hours)

                // Le journal rejoue les filtres courants dès que les stats sont là
                // (le collect des filtres ne se réveille pas seul si rien n'a changé).
                _uiState.value = _uiState.value.copy(
                    configured = true,
                    loading = false,
                    health = health,
                    stats = stats,
                )
                loadJournal()

                try {
                    val alerts = api.alerts(base.first, base.second, hours)
                    _uiState.value = _uiState.value.copy(alerts = alerts)
                } catch (_: Exception) {
                    // best effort : les alertes se rechargeront au prochain refresh
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = friendly(e))
            }
        }
    }

    private fun loadJournal() {
        val base = currentConfig ?: return
        journalJob?.cancel()
        journalJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(journalLoading = true)
            try {
                val query = TrafficQuery(
                    hours = _uiState.value.hours,
                    limit = PAGE_SIZE,
                    offset = 0,
                    ip = null,
                    method = null,
                    source = _uiState.value.source,
                    suspicious = if (_uiState.value.suspiciousOnly) 1 else null,
                    status = null,
                    search = _uiState.value.search.ifBlank { null },
                )
                val list = api.requests(base.first, base.second, query)
                _uiState.value = _uiState.value.copy(journal = list, journalLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(journalLoading = false, error = friendly(e))
            }
        }
    }

    private fun friendly(e: Exception): String = when (e) {
        is TrafficApiDisabledException -> e.message ?: "API /ti-api désactivée côté serveur."
        is TrafficAuthException -> e.message ?: "Token traffic-inspector invalide (401)."
        is TrafficApiException -> e.message ?: "Erreur de l'API traffic-inspector."
        else -> e.message ?: "Connexion à traffic-inspector impossible."
    }

    private companion object {
        const val PAGE_SIZE = 50
    }
}
