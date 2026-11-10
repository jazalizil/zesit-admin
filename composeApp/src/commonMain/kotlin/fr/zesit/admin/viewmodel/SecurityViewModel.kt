package fr.zesit.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.zesit.admin.di.AppContainer
import fr.zesit.admin.security.SecurityApiDisabledException
import fr.zesit.admin.security.SecurityApiException
import fr.zesit.admin.security.SecurityAuthException
import fr.zesit.admin.security.SecurityFindingsList
import fr.zesit.admin.security.SecurityHealth
import fr.zesit.admin.security.SecurityScans
import fr.zesit.admin.security.SecurityScanConflictException
import fr.zesit.admin.security.SecuritySummary
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

data class SecurityUiState(
    val configured: Boolean = false,
    /** Dernière réponse /sc-api/health (api_enabled = SC_API_TOKEN configuré). */
    val health: SecurityHealth? = null,
    val summary: SecuritySummary? = null,
    /** Historique des cycles de scan (meilleur effort). */
    val scans: SecurityScans? = null,
    /** Fiche vulnérabilités (dialogue) : image sélectionnée. */
    val detail: SecurityFindingsList? = null,
    val detailSeverity: String? = null,
    val detailSearch: String = "",
    val detailLoading: Boolean = false,
    val detailLoadingMore: Boolean = false,
    /** Feedback entre POST /sc-api/scan et la fin du scan observée côté serveur. */
    val scanRequested: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
)

/**
 * Panneau « Sécurité » — consomme l'API JSON security-scanner (`/sc-api`,
 * zesit-vps/security-scanner, VPN-only) : résumé des vulnérabilités par image
 * (Trivy, deltas au dernier scan), fiche détaillée par image et scan manuel.
 * Réglages de connexion (URL ; le token SC_API_TOKEN vient de l'environnement,
 * installé dans les secrets de l'app) édités depuis la page même.
 */
@OptIn(FlowPreview::class)
class SecurityViewModel(private val container: AppContainer) : ViewModel() {

    private val api get() = container.securityApi

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    /** Réglages enregistrés (pré-remplissage du panneau de connexion). */
    val settings get() = container.settingsRepository.appSettings

    /** Environnement d'exécution (plateforme, valeurs par défaut communes). */
    val env get() = container.env

    /** (baseUrl, token) courants — toute modification relance le chargement. */
    private var currentConfig: Pair<String, String>? = null

    /** L'auto-configuration ne s'applique qu'une fois par session : une Déconnexion explicite reste effective jusqu'au prochain lancement. */
    private var autoConfigAttempted = false

    /** Recherche débouncée de la fiche détaillée : (sévérité, texte). */
    private val detailFilters = MutableStateFlow<Pair<String?, String>>(null to "")

    private var loadJob: Job? = null
    private var detailJob: Job? = null
    private var scanJob: Job? = null

    init {
        viewModelScope.launch {
            container.settingsRepository.appSettings.collectLatest { s ->
                // Auto-configuration (une fois par session) : si rien n'est saisi
                // mais que l'app dispose du token SC (env installé dans les
                // secrets), on branche l'URL par défaut de l'env — la page se
                // connecte seule.
                if (!autoConfigAttempted && !s.isSecurityConfigured) {
                    val token = container.secrets.securityApiToken()
                    if (!token.isNullOrBlank()) {
                        autoConfigAttempted = true
                        container.settingsRepository.save(
                            s.copy(securityBaseUrl = container.env.securityBaseUrlDefault),
                        )
                        return@collectLatest
                    }
                }
                val cfg = s.normalizedSecurityBaseUrl to container.secrets.securityApiToken().orEmpty()
                if (cfg != currentConfig) {
                    currentConfig = cfg
                    if (cfg.first.isBlank()) {
                        _uiState.value = SecurityUiState()
                    } else {
                        _uiState.value = _uiState.value.copy(configured = true)
                        reload()
                    }
                }
            }
        }
        viewModelScope.launch {
            detailFilters.debounce(400).collectLatest {
                if (_uiState.value.detail != null || _uiState.value.detailLoading) loadDetail()
            }
        }
    }

    // --- Connexion / déconnexion ---

    /** Connexion depuis la page Sécurité : URL et token viennent de l'environnement (aucune saisie). */
    fun connect() {
        val current = container.settingsRepository.appSettings.value
        container.settingsRepository.save(current.copy(securityBaseUrl = container.env.securityBaseUrlDefault.trim().trimEnd('/')))
    }

    /** true si le token SC est disponible (installé depuis l'environnement). */
    fun hasSecretToken(): Boolean = !container.secrets.securityApiToken().isNullOrBlank()

    /** Déconnexion (bouton) : efface les réglages security-scanner. */
    fun disconnect() {
        val current = container.settingsRepository.appSettings.value
        container.settingsRepository.save(current.copy(securityBaseUrl = ""))
    }

    // --- Fiche détaillée (dialogue) ---

    fun openDetail(imageId: String) {
        val base = currentConfig ?: return
        detailJob?.cancel()
        // Filtres réinitialisés à l'ouverture d'une nouvelle image.
        _uiState.value = _uiState.value.copy(
            detail = null,
            detailSeverity = null,
            detailSearch = "",
            detailLoading = true,
        )
        detailJob = viewModelScope.launch {
            try {
                val list = api.findings(base.first, base.second, imageId, limit = DETAIL_PAGE)
                _uiState.value = _uiState.value.copy(detail = list, detailLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(detailLoading = false, error = friendly(e))
            }
        }
    }

    fun setDetailSeverity(severity: String?) {
        if (_uiState.value.detailSeverity == severity) return
        _uiState.value = _uiState.value.copy(detailSeverity = severity)
        loadDetail()
    }

    fun setDetailSearch(search: String) {
        _uiState.value = _uiState.value.copy(detailSearch = search)
        detailFilters.value = _uiState.value.detailSeverity to search
    }

    fun loadMoreDetail() {
        val detail = _uiState.value.detail ?: return
        val base = currentConfig ?: return
        if (_uiState.value.detailLoadingMore) return
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(detailLoadingMore = true)
            try {
                val more = api.findings(
                    base.first, base.second,
                    image = detail.image.image_id,
                    severity = _uiState.value.detailSeverity,
                    q = _uiState.value.detailSearch.ifBlank { null },
                    limit = DETAIL_PAGE,
                    offset = detail.findings.size,
                )
                _uiState.value = _uiState.value.copy(
                    detail = detail.copy(
                        findings = detail.findings + more.findings,
                        limit = detail.findings.size + more.findings.size,
                        offset = detail.offset,
                    ),
                    detailLoadingMore = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(detailLoadingMore = false, error = friendly(e))
            }
        }
    }

    fun closeDetail() {
        detailJob?.cancel()
        _uiState.value = _uiState.value.copy(
            detail = null, detailLoading = false, detailLoadingMore = false,
            detailSeverity = null, detailSearch = "",
        )
    }

    // --- Scan manuel ---

    /**
     * POST /sc-api/scan puis surveille la fin du scan côté serveur (health) :
     * rafraîchit le résumé dès qu'il est terminé. 409 = un scan tourne déjà.
     */
    fun triggerScan() {
        val base = currentConfig ?: return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            try {
                try {
                    api.triggerScan(base.first, base.second)
                } catch (_: SecurityScanConflictException) {
                    // Un scan tourne déjà : on le surveille de la même façon.
                }
                _uiState.value = _uiState.value.copy(scanRequested = true)
                // Poll léger : le run est ramassé par le master en <= 5 s, puis
                // durée variable selon les images — on borne à ~1 h.
                repeat(MAX_SCAN_POLLS) {
                    delay(POLL_SECONDS * 1000L)
                    if (!api.health(base.first).scanning) {
                        _uiState.value = _uiState.value.copy(scanRequested = false)
                        reload()
                        return@launch
                    }
                }
                _uiState.value = _uiState.value.copy(scanRequested = false)
                reload()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(scanRequested = false, error = friendly(e))
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // --- Chargements ---

    /** Recharge santé + résumé ; l'historique suit en meilleur effort. */
    fun reload() {
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
                            "security-scanner signale un problème."
                        } else {
                            "API /sc-api désactivée côté serveur (SC_API_TOKEN vide)."
                        },
                    )
                    return@launch
                }
                val summary = api.summary(base.first, base.second)
                _uiState.value = _uiState.value.copy(
                    configured = true,
                    loading = false,
                    health = health,
                    summary = summary,
                )
                // Historique : meilleur effort (s'il échoue, pas d'erreur bloquante).
                try {
                    val scans = api.scans(base.first, base.second, limit = 12)
                    _uiState.value = _uiState.value.copy(scans = scans)
                } catch (_: Exception) {
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = friendly(e))
            }
        }
    }

    private fun loadDetail() {
        val base = currentConfig ?: return
        val imageId = _uiState.value.detail?.image?.image_id ?: return
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(detailLoading = true)
            try {
                val list = api.findings(
                    base.first, base.second,
                    image = imageId,
                    severity = _uiState.value.detailSeverity,
                    q = _uiState.value.detailSearch.ifBlank { null },
                    limit = DETAIL_PAGE,
                )
                _uiState.value = _uiState.value.copy(detail = list, detailLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(detailLoading = false, error = friendly(e))
            }
        }
    }

    private fun friendly(e: Exception): String = when (e) {
        is SecurityApiDisabledException -> e.message ?: "API /sc-api désactivée côté serveur."
        is SecurityAuthException ->
            if (container.secrets.securityApiToken() == null) {
                "Aucun token SC dans l'environnement : lancez l'app avec la variable " +
                    "SC_API_TOKEN définie (export SC_API_TOKEN=… puis relancez), valeur du " +
                    "SC_API_TOKEN du .env du VPS."
            } else {
                e.message ?: "Token security-scanner refusé (401) — SC_API_TOKEN ne correspond pas à celui du serveur."
            }
        is SecurityApiException -> e.message ?: "Erreur de l'API security-scanner."
        else -> e.message ?: "Connexion à security-scanner impossible (VPN WireGuard requis)."
    }

    private companion object {
        const val DETAIL_PAGE = 50
        const val POLL_SECONDS = 6L
        const val MAX_SCAN_POLLS = 600
    }
}
