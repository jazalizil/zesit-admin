package fr.zesit.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.zesit.admin.di.AppContainer
import fr.zesit.admin.di.KpiSession
import fr.zesit.admin.di.KpiSessionState
import fr.zesit.admin.kpi.KpiFmt
import fr.zesit.admin.kpi.KpiInputs
import fr.zesit.admin.kpi.KpiModelSummary
import fr.zesit.admin.kpi.KpiResponse
import fr.zesit.admin.kpi.KpiAuthException
import fr.zesit.admin.kpi.KpiApiException
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
 * changement d'hypothèse, CRUD des modèles enregistrés. Auth déléguée à [KpiSession].
 */
class KpiViewModel(private val container: AppContainer) : ViewModel() {

    private val api get() = container.kpiApi
    private val hypothesesStore = container.kpiHypothesesStore
    private val session: KpiSession = container.kpiSession

    private val _uiState = MutableStateFlow(KpiUiState())
    val uiState: StateFlow<KpiUiState> = _uiState.asStateFlow()

    /** Pipeline de recalcul : chaque publication déclenche un refresh débouncé. */
    private val inputsFlow = MutableStateFlow<KpiInputs?>(null)

    /** Hypothèses courantes du modèle chargé (consommé par la section Réel/Prévisionnel). */
    internal val currentInputs: StateFlow<KpiInputs?> = inputsFlow.asStateFlow()

    private var lastComputed: KpiInputs? = null

    init {
        viewModelScope.launch {
            session.state.collectLatest { st -> onSessionState(st) }
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

    private fun onSessionState(st: KpiSessionState) {
        val previousConnected = _uiState.value.connected
        _uiState.value = _uiState.value.copy(
            configured = st.configured,
            connected = st.connected,
            userName = st.userName,
            error = when {
                !st.connected -> st.error
                else -> null
            },
        )
        // (Re)connexion : hydrate les hypothèses puis charge la liste des modèles.
        if (st.connected && !previousConnected) {
            viewModelScope.launch { hydrate() }
        }
    }

    /** Session valide → hypothèses locales sinon défaut serveur, puis recalcul + modèles. */
    private suspend fun hydrate() {
        try {
            val inputs = _uiState.value.inputs ?: (hypothesesStore.load() ?: defaultsFromServer())
            lastComputed = null // force le recalcul réseau du modèle
            applyInputs(inputs) // publie dans inputsFlow -> refresh débouncé
            reloadModels()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = friendly(e))
        }
    }

    private suspend fun defaultsFromServer(): KpiInputs =
        session.authed { base, token -> api.defaults(base, token) }

    // --- Edition des hypothèses (optimiste, comme le web) ---

    internal fun setInput(key: KpiInputKey, value: Double) = updateInputs { key.apply(it, value) }

    internal fun setFormat(index: Int, key: KpiFormatKey, value: Double) = updateInputs { inputs ->
        inputs.copy(formats = inputs.formats.mapIndexed { i, f -> if (i == index) key.apply(f, value) else f })
    }

    /** Réinitialise aux hypothèses par défaut du serveur (source de vérité). */
    fun reset() {
        viewModelScope.launch {
            try {
                val dflt = defaultsFromServer()
                applyInputs(dflt)
                notice(true, "Hypothèses réinitialisées aux valeurs par défaut.")
            } catch (e: Exception) {
                notice(false, friendly(e))
            }
        }
    }

    // --- Modèles enregistrés (/api/models) ---

    fun selectModel(id: String?) {
        _uiState.value = _uiState.value.copy(selectedModelId = id)
    }

    /** Charge le modèle sélectionné : hypothèses + résultats stockés par l'API. */
    fun loadSelectedModel() {
        val id = _uiState.value.selectedModelId ?: return
        launchBusy {
            val detail = session.authed { base, token -> api.modelDetail(base, token, id) }
            lastComputed = detail.inputs
            hypothesesStore.save(detail.inputs)
            inputsFlow.value = detail.inputs
            _uiState.value = _uiState.value.copy(
                inputs = detail.inputs,
                model = detail.resultats,
                dirty = false,
                notice = KpiNotice(true, "Hypothèses du modèle « ${detail.name} » chargées."),
            )
        }
    }

    /**
     * Enregistre l'état courant : mise à jour du modèle sélectionné s'il y en a un,
     * création sinon (l'API recalcule et stocke les résultats).
     */
    fun saveCurrentModel(name: String, description: String) {
        val inputs = _uiState.value.inputs ?: return
        val id = _uiState.value.selectedModelId
        launchBusy {
            val saved = session.authed { base, token ->
                if (id == null) {
                    api.createModel(base, token, name, description, inputs)
                } else {
                    api.updateModel(base, token, id, name, description, inputs)
                }
            }
            lastComputed = saved.inputs
            hypothesesStore.save(saved.inputs)
            inputsFlow.value = saved.inputs
            _uiState.value = _uiState.value.copy(
                inputs = saved.inputs,
                model = saved.resultats,
                dirty = false,
                selectedModelId = saved.id,
                notice = KpiNotice(true, "Modèle « ${saved.name} » enregistré."),
            )
            reloadModels() // rafraîchit la liste (KPIs/dates côté serveur)
        }
    }

    fun deleteSelectedModel() {
        val id = _uiState.value.selectedModelId ?: return
        launchBusy {
            session.authed { base, token -> api.deleteModel(base, token, id) }
            _uiState.value = _uiState.value.copy(selectedModelId = null)
            reloadModels()
            notice(true, "Modèle supprimé.")
        }
    }

    // --- Session ---

    /** Réglages KPI enregistrés (pré-remplissage du panneau de connexion). */
    val settings get() = container.settingsRepository.appSettings

    /**
     * Connexion depuis la page KPI (état non connecté) : enregistre les
     * identifiants puis relance la session (login automatique par KpiSession).
     */
    fun connectKpi(baseUrl: String, user: String, password: String) {
        container.settingsRepository.save(
            container.settingsRepository.appSettings.value.copy(
                kpiBaseUrl = baseUrl.trim().trimEnd('/'),
                kpiUser = user.trim(),
                kpiPassword = password,
            ),
        )
        session.reconnect()
    }

    fun logout() = session.logout()

    /** Reconnexion explicite (après logout ou erreur d'identifiants). */
    fun reconnect() = session.reconnect()

    fun clearNotice() {
        _uiState.value = _uiState.value.copy(notice = null)
    }

    // --- Internes ---

    private suspend fun refresh() {
        val base = session.baseUrl() ?: return
        val inputs = _uiState.value.inputs ?: return
        // Optimistic : si un modèle chargé/enregistré correspond exactement aux inputs,
        // inutile de recalculer (l'API vient de nous les renvoyer).
        if (inputs == lastComputed && _uiState.value.model != null) return
        _uiState.value = _uiState.value.copy(loading = true)
        try {
            val response = session.authed { base, token -> api.model(base, token, inputs) }
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
        val base = session.baseUrl() ?: return
        _uiState.value = _uiState.value.copy(modelsLoading = true)
        try {
            val list = session.authed { base, token -> api.models(base, token) }
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

    /** Applique un « update » optimiste aux hypothèses + persiste + déclenche le recalcul. */
    private fun updateInputs(transform: (KpiInputs) -> KpiInputs) {
        val current = _uiState.value.inputs ?: return
        val next = transform(current)
        if (next == current) return
        hypothesesStore.save(next)
        _uiState.value = _uiState.value.copy(inputs = next, dirty = true)
        inputsFlow.value = next
    }

    /** lance une action serveur avec gestion du busy + erreur/notice. */
    private fun launchBusy(action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(modelBusy = true)
            try {
                action()
                _uiState.value = _uiState.value.copy(modelBusy = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(modelBusy = false, notice = KpiNotice(false, friendly(e)))
            }
        }
    }

    private fun notice(ok: Boolean, text: String) {
        _uiState.value = _uiState.value.copy(notice = KpiNotice(ok, text))
    }

    private fun friendly(e: Exception): String = when (e) {
        is KpiApiException, is KpiAuthException -> e.message ?: "Erreur API KPI."
        else -> e.message ?: "Erreur de connexion au serveur KPI."
    }

    private companion object {
        const val REFRESH_DEBOUNCE_MS: Long = 250
    }
}
