package fr.zesit.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.zesit.admin.di.AppContainer
import fr.zesit.admin.kpi.KpiFmt
import fr.zesit.admin.kpi.KpiInputs
import fr.zesit.admin.kpi.KpiModelSummary
import fr.zesit.admin.kpi.KpiResponse
import fr.zesit.admin.kpi.KpiAuthException
import fr.zesit.admin.kpi.KpiApiException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Clés des hypothèses globales (/slider ou champs) — miroir de GLOBAL_KEYS du web. */
internal enum class KpiInputKey(val label: String, val apply: (KpiInputs, Double) -> KpiInputs) {
    CASSE("Casse et invendus", { i, v -> i.copy(casse = v) }),
    PART_GMS("Part GMS (PET) — tous formats", { i, v -> i.copy(partGMS = v) }),
    PATE("Pâte de citron (€/kg)", { i, v -> i.copy(pate = v) }),
    FRET("Fret import pâte (€/kg)", { i, v -> i.copy(fret = v) }),
    SUCRE("Sucre bio (€/kg)", { i, v -> i.copy(sucre = v) }),
    VANILLE("Sucre vanillé (€/sachet)", { i, v -> i.copy(vanille = v) }),
    TRANSPORT("Transport / stockage (locaux)", { i, v -> i.copy(transport = v) }),
    MARKETING("Marketing", { i, v -> i.copy(marketing = v) }),
    COMPTA("Compta / assurance", { i, v -> i.copy(compta = v) }),
    LOYER("Loyer (local)", { i, v -> i.copy(loyer = v) }),
    REMU("Rémunération fondateur", { i, v -> i.copy(remu = v) }),
    LABO_PAR_LOT("Analyses labo par lot (€)", { i, v -> i.copy(laboParLot = v) }),
    LABO_LOTS_AN("Lots de pâte importés / an", { i, v -> i.copy(laboLotsAn = v) }),
    ONE_SHOT("Frais de création (€)", { i, v -> i.copy(oneShot = v) }),
    APPORT("Apport (€)", { i, v -> i.copy(apport = v) }),
}

/** Champs éditables d'un format de bouteille. */
internal enum class KpiFormatKey(val apply: (KpiFmt, Double) -> KpiFmt) {
    QTY({ f, v -> f.copy(qty = v) }),
    PRIX_CHR({ f, v -> f.copy(prixCHR = v) }),
    PRIX_GMS({ f, v -> f.copy(prixGMS = v) }),
    COPACK_VERRE({ f, v -> f.copy(copackVerre = v) }),
    COPACK_PET({ f, v -> f.copy(copackPET = v) }),
}

data class KpiNotice(val ok: Boolean, val text: String)

data class KpiUiState(
    /** Serveur KPI + utilisateur renseignés (via Réglages). */
    val configured: Boolean = false,
    /** Session valide (login OK). */
    val connected: Boolean = false,
    val userName: String? = null,
    val inputs: KpiInputs? = null,
    val model: KpiResponse? = null,
    /** Recalcul API en cours. */
    val loading: Boolean = false,
    /** true si les hypothèses ont changé depuis le dernier résultat reçu (optimiste). */
    val dirty: Boolean = false,
    val error: String? = null,
    val notice: KpiNotice? = null,
    // Modèles enregistrés (persistance serveur)
    val models: List<KpiModelSummary> = emptyList(),
    val modelsLoading: Boolean = false,
    val selectedModelId: String? = null,
    val modelBusy: Boolean = false,
)

/**
 * Portage du store Zustand du dashboard web (zesit-kpi/apps/web/src/store/model.ts) :
 * hydratation (hypothèses locales sinon `/api/defaults`), recalcul débouncé à chaque
 * changement d'hypothèse, re-login automatique sur 401, CRUD des modèles enregistrés.
 */
class KpiViewModel(private val container: AppContainer) : ViewModel() {

    private val api get() = container.kpiApi
    private val sessionStore = container.kpiSessionStore
    private val hypothesesStore = container.kpiHypothesesStore

    private val _uiState = MutableStateFlow(KpiUiState())
    val uiState: StateFlow<KpiUiState> = _uiState.asStateFlow()

    /** Pipeline de recalcul : chaque publication déclenche un refresh débouncé. */
    private val inputsFlow = MutableStateFlow<KpiInputs?>(null)

    /** (baseUrl, user, password) — une nouvelle valeur (≠) relance la connexion. */
    private var config: Triple<String, String, String>? = null
    private var token: String? = null
    private var lastComputed: KpiInputs? = null
    private var sessionClosed = false

    init {
        viewModelScope.launch {
            container.settingsRepository.appSettings.collectLatest { settings ->
                val newConfig = Triple(
                    settings.normalizedKpiBaseUrl,
                    settings.kpiUser.trim(),
                    settings.kpiPassword,
                )
                if (newConfig != config) {
                    config = newConfig
                    sessionClosed = false
                    boot()
                }
            }
        }
        viewModelScope.launch {
            inputsFlow.collectLatest { inputs ->
                if (inputs != null) {
                    delay(REFRESH_DEBOUNCE_MS)
                    refresh()
                }
            }
        }
    }

    // --- Edition des hypothèses (optimiste, comme le web) ---

    internal fun setInput(key: KpiInputKey, value: Double) = updateInputs { key.apply(it, value) }

    internal fun setFormat(index: Int, key: KpiFormatKey, value: Double) = updateInputs { inputs ->
        inputs.copy(formats = inputs.formats.mapIndexed { i, f -> if (i == index) key.apply(f, value) else f })
    }

    /** Applique un « update » optimiste aux hypothèses + persiste + déclenche le recalcul. */
    private fun updateInputs(transform: (KpiInputs) -> KpiInputs) {
        val current = _uiState.value.inputs ?: return
        val next = transform(current)
        if (next == current) return
        hypothesesStore.save(next)
        _uiState.value = _uiState.value.copy(inputs = next, dirty = true)
        inputsFlow.value = next
    }

    /** Réinitialise aux hypothèses par défaut du serveur (source de vérité). */
    fun reset() {
        val base = baseUrl() ?: return
        launchAuth {
            val dflt = authed { api.defaults(base, it) }
            applyInputs(dflt)
            notice(true, "Hypothèses réinitialisées aux valeurs par défaut.")
        }
    }

    // --- Modèles enregistrés (/api/models) ---

    fun selectModel(id: String?) {
        _uiState.value = _uiState.value.copy(selectedModelId = id)
    }

    /** Charge le modèle sélectionné : hypothèses + résultats stockés par l'API. */
    fun loadSelectedModel() {
        val base = baseUrl() ?: return
        val id = _uiState.value.selectedModelId ?: return
        launchAuth {
            val detail = authed { api.modelDetail(base, it, id) }
            lastComputed = detail.inputs
            hypothesesStore.save(detail.inputs)
            _uiState.value = _uiState.value.copy(
                inputs = detail.inputs,
                model = detail.resultats,
                dirty = false,
                modelBusy = false,
                error = null,
                notice = KpiNotice(true, "Hypothèses du modèle « ${detail.name} » chargées."),
            )
            inputsFlow.value = detail.inputs // == lastComputed → pas de recalcul réseau
        }
    }

    /**
     * Enregistre l'état courant : mise à jour du modèle sélectionné s'il y en a un,
     * création sinon (l'API recalcule et stocke les résultats).
     */
    fun saveCurrentModel(name: String, description: String) {
        val base = baseUrl() ?: return
        val inputs = _uiState.value.inputs ?: return
        val id = _uiState.value.selectedModelId
        launchAuth {
            val saved = authed { token ->
                if (id == null) {
                    api.createModel(base, token, name, description, inputs)
                } else {
                    api.updateModel(base, token, id, name, description, inputs)
                }
            }
            lastComputed = saved.inputs
            hypothesesStore.save(saved.inputs)
            _uiState.value = _uiState.value.copy(
                inputs = saved.inputs,
                model = saved.resultats,
                dirty = false,
                selectedModelId = saved.id,
                modelBusy = false,
                notice = KpiNotice(true, "Modèle « ${saved.name} » enregistré."),
            )
            reloadModels() // rafraîchit la liste (KPIs/dates côté serveur)
        }
    }

    fun deleteSelectedModel() {
        val base = baseUrl() ?: return
        val id = _uiState.value.selectedModelId ?: return
        launchAuth {
            authed { api.deleteModel(base, it, id) }
            _uiState.value = _uiState.value.copy(selectedModelId = null, modelBusy = false)
            reloadModels()
            notice(true, "Modèle supprimé.")
        }
    }

    // --- Session ---

    fun logout() {
        viewModelScope.launch {
            val base = config?.first ?: return@launch
            token?.let { t -> runCatching { api.logout(base, t) } }
            token = null
            sessionStore.set(null)
            sessionClosed = true
            _uiState.value = _uiState.value.copy(
                connected = false,
                userName = null,
                error = "Session KPI fermée — reconnectez-vous.",
            )
        }
    }

    /** Reconnexion explicite (après logout ou erreur d'identifiants). */
    fun reconnect() {
        viewModelScope.launch {
            sessionClosed = false
            boot()
        }
    }

    fun clearNotice() {
        _uiState.value = _uiState.value.copy(notice = null)
    }

    // --- Internes ---

    /** Connexion/refresh initial : session, hypothèses, modèles. */
    private suspend fun boot() {
        val (base, user, _) = config ?: return
        if (base.isBlank() || user.isBlank()) {
            token = null
            sessionStore.set(null)
            _uiState.value = KpiUiState(configured = false)
            return
        }
        _uiState.value = _uiState.value.copy(configured = true, connected = false, error = null)
        try {
            val name = authed { api.me(base, it) }
            _uiState.value = _uiState.value.copy(connected = true, userName = name)
            val inputs = hypothesesStore.load() ?: authed { api.defaults(base, it) }
            lastComputed = null // force le recalcul réseau du modèle
            applyInputs(inputs)
            refresh() // modèle initial
            reloadModels()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                connected = false,
                error = friendly(e),
            )
        }
    }

    /**
     * Exécute `block` avec un token valide : re-login automatique sur 401
     * (une seule tentative, sinon l'erreur remonte à l'UI).
     */
    private suspend fun <T> authed(block: suspend (String) -> T): T {
        val (base, user, pass) = config ?: throw KpiApiException("Serveur KPI non configuré.")
        if (sessionClosed) throw KpiAuthException("Session KPI fermée — reconnectez-vous.")
        var current = token ?: sessionStore.get()
        if (current == null) {
            current = api.login(base, user, pass).also { sessionStore.set(it); token = it }
        }
        return try {
            block(current)
        } catch (_: KpiAuthException) {
            val fresh = api.login(base, user, pass).also { sessionStore.set(it); token = it }
            try { block(fresh) } catch (_: KpiAuthException) { throw KpiAuthException("Identifiants KPI invalides.") }
        }
    }

    /** lance une action authentifiée avec gestion du busy + erreur/notice. */
    private fun launchAuth(action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(modelBusy = true)
            try {
                action()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(modelBusy = false, notice = KpiNotice(false, friendly(e)))
            }
        }
    }

    private suspend fun refresh() {
        val base = baseUrl() ?: return
        val inputs = _uiState.value.inputs ?: return
        // Optimistic : si un modèle chargé/enregistré correspond exactement aux inputs,
        // inutile de recalculer (l'API vient de nous les renvoyer).
        if (inputs == lastComputed && _uiState.value.model != null) return
        _uiState.value = _uiState.value.copy(loading = true)
        try {
            val response = authed { api.model(base, it, inputs) }
            if (_uiState.value.inputs == inputs) {
                lastComputed = inputs
                _uiState.value = _uiState.value.copy(model = response, loading = false, dirty = false, error = null)
            } else {
                _uiState.value = _uiState.value.copy(loading = false)
            }
        } catch (e: Exception) {
            if (_uiState.value.inputs == inputs) {
                _uiState.value = _uiState.value.copy(loading = false, error = friendly(e))
            }
        }
    }

    /** Recharge la liste des modèles (best effort, sans erreur bloquante). */
    private suspend fun reloadModels() {
        val base = baseUrl() ?: return
        _uiState.value = _uiState.value.copy(modelsLoading = true)
        try {
            val list = authed { api.models(base, it) }
            _uiState.value = _uiState.value.copy(
                models = list,
                modelsLoading = false,
                selectedModelId = _uiState.value.selectedModelId?.takeIf { sel -> list.any { it.id == sel } },
            )
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(modelsLoading = false)
        }
    }

    private fun applyInputs(inputs: KpiInputs) {
        hypothesesStore.save(inputs)
        _uiState.value = _uiState.value.copy(inputs = inputs, dirty = lastComputed != inputs)
        inputsFlow.value = inputs
    }

    private fun baseUrl(): String? {
        val current = config ?: return null
        return current.first.takeIf { it.isNotBlank() && current.second.isNotBlank() }
    }

    private fun notice(ok: Boolean, text: String) {
        _uiState.value = _uiState.value.copy(modelBusy = false, notice = KpiNotice(ok, text))
    }

    private fun friendly(e: Exception): String = when (e) {
        is KpiApiException, is KpiAuthException -> e.message ?: "Erreur API KPI."
        else -> e.message ?: "Erreur de connexion au serveur KPI."
    }

    private companion object {
        const val REFRESH_DEBOUNCE_MS: Long = 250
    }
}
