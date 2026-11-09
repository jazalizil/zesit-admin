package fr.zesit.admin.ui.kpi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.zesit.admin.kpi.KpiFmt
import fr.zesit.admin.kpi.KpiInputs
import fr.zesit.admin.kpi.KpiResponse
import fr.zesit.admin.ui.common.KpiAuthPanel
import fr.zesit.admin.ui.common.StatePanel
import fr.zesit.admin.viewmodel.KpiUiState
import fr.zesit.admin.viewmodel.KpiViewModel
import kotlin.math.abs

private val TABS = listOf("📊 Année 1", "🌡️ Sensibilité", "🧾 Détail mensuel")

/**
 * Section KPI du dashboard — portage Compose de la page DashboardPage.tsx
 * (zesit-kpi/apps/web) : KPI cards, formats, hypothèses, graphiques, tableau
 * mensuel et modèles enregistrés.
 *
 * L'authentification au serveur KPI vit ici (état non connecté) : le panneau
 * de connexion remplace l'ancien renvoi vers les Réglages ; une fois connecté,
 * l'en-tête expose une Déconnexion (power_off).
 *
 * Composant « expect » : l'agencement par plateforme vit dans l'`actual`
 * (desktop : desktopMain, fichier par composant) ; l'agencement commun de
 * référence est [DefaultKpiScreen].
 */
@Composable
expect fun KpiScreen(
    viewModel: KpiViewModel,
    modifier: Modifier = Modifier,
)

@Composable
internal fun DefaultKpiScreen(
    viewModel: KpiViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    when {
        // Non connecté (jamais configuré, déconnexion ou identifiants refusés) :
        // panneau d'authentification sur la page même. Une session en cours de
        // login (configurée, sans erreur) affiche une progression.
        !state.connected -> KpiAuthPanel(
            savedBaseUrl = settings.kpiBaseUrl,
            savedUser = settings.kpiUser,
            savedPassword = settings.kpiPassword,
            connecting = state.configured && state.error == null,
            error = state.error,
            onConnect = viewModel::connectKpi,
            modifier = modifier,
        )
        state.inputs == null || state.model == null -> LoadingState(modifier)
        else -> DashboardContent(state, viewModel, modifier)
    }
}

@Composable
private fun LoadingState(modifier: Modifier) {
    StatePanel(modifier) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Chargement du prévisionnel…", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DashboardContent(
    state: KpiUiState,
    viewModel: KpiViewModel,
    modifier: Modifier,
) {
    val model = state.model ?: return
    val inputs = state.inputs ?: return

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 1180.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Header(state, viewModel)
                ModelsBar(
                    state = state,
                    onSelect = viewModel::selectModel,
                    onLoad = viewModel::loadSelectedModel,
                    onSave = viewModel::saveCurrentModel,
                    onDelete = viewModel::deleteSelectedModel,
                )
                KpiCardsGrid(model, inputs)

                Text(
                    "Formats de bouteilles — quantités et prix (quantité 0 = format inactif)",
                    style = MaterialTheme.typography.titleSmall,
                )
                FormatsGrid(inputs, model, viewModel)

                BoxWithConstraints {
                    if (maxWidth >= 900.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            HypothesesPanel(
                                inputs = inputs,
                                model = model,
                                onSet = viewModel::setInput,
                                modifier = Modifier.widthIn(max = 380.dp),
                            )
                            ChartsArea(model, inputs, Modifier.weight(1f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            HypothesesPanel(inputs = inputs, model = model, onSet = viewModel::setInput)
                            ChartsArea(model, inputs, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: KpiUiState, viewModel: KpiViewModel) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChipItem("Prévisionnel année 1")
            AssistChipItem("Duo verre / PET · 25 / 33 / 50 cl")
            if (state.loading || state.dirty) AssistChipItem("recalcul…")
            if (state.connected) AssistChipItem("Connecté${state.userName?.let { " ($it)" } ?: ""}")
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Dashboard prévisionnel — Citronnade tunisienne",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Choisissez vos formats et quantités, ajustez les prix : tout est recalculé en temps réel par l'API. " +
                "Les hypothèses sont sauvegardées automatiquement ; les scénarios peuvent être enregistrés sur le serveur.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = viewModel::reset, enabled = !state.modelBusy) { Text("Réinitialiser") }
            OutlinedButton(onClick = viewModel::logout) {
                Icon(
                    Icons.Default.PowerOff,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Déconnexion")
            }
            state.error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            state.notice?.let { n ->
                Text(
                    n.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (n.ok) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun AssistChipItem(label: String) {
    OutlinedCard(colors = CardDefaults.outlinedCardColors()) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

private data class KpiCardData(
    val label: String,
    val value: String,
    val sub: String?,
    val tone: KpiTone,
)

@Composable
private fun KpiCardsGrid(model: KpiResponse, inputs: KpiInputs) {
    val formatsActifs = inputs.formats.filter { it.qty > 0 }
    val cards = listOf(
        KpiCardData(
            "CA année 1 (HT)", eur(model.totals.caTotal),
            "${fmtInt(model.totals.vendu)} bouteilles vendues", KpiTone.NEUTRAL,
        ),
        KpiCardData(
            "Résultat net année 1", eur(model.totals.resultat),
            "Marge s/ coûts var. : ${eur(model.totals.marge)}",
            if (model.totals.resultat >= 0) KpiTone.GOOD else KpiTone.BAD,
        ),
        KpiCardData(
            "Année 2 plein régime", eur(model.annee2), null,
            if (model.annee2 >= 0) KpiTone.GOOD else KpiTone.BAD,
        ),
        KpiCardData(
            "Trésorerie fin d'année", eur(model.tresoFin),
            "Point bas : ${eur(model.tresoMin)}",
            if (model.tresoMin >= 0) KpiTone.GOOD else KpiTone.BAD,
        ),
        KpiCardData(
            "Volume plein régime", "${fmtInt(model.volPlen)} L/mois",
            "${fmtInt(model.qtyTot)} btl/mois · " +
                formatsActifs.joinToString(" + ") { "${it.cl} cl" }.ifEmpty { "aucun format actif" },
            KpiTone.NEUTRAL,
        ),
        KpiCardData(
            "Pâte de citron nécessaire", "${fmtInt(model.pateKgMois)} kg/mois",
            "Année 1 : ${fmtInt(model.pateKgAn1)} kg · année 2 : ${fmtInt(model.pateKgAn2)} kg" +
                " · coût rendu ${eur(model.pateCoutMois)}/mois",
            KpiTone.NEUTRAL,
        ),
        KpiCardData(
            "Seuil de rentabilité (estimé)",
            model.seuilL?.let { "${fmtInt(it)} L/mois" } ?: "—",
            model.seuilL?.let { "Charges : ${eur(model.chargesMois)}/mois · à mix et prix constants" } ?: "—",
            if (model.totals.resultat >= 0) KpiTone.GOOD else KpiTone.BAD,
        ),
    )

    ResponsiveColumns(items = cards, minColumnWidth = 250.dp) { card ->
        KpiCard(card.label, card.value, card.sub, card.tone, Modifier.fillMaxWidth())
    }
}

@Composable
private fun FormatsGrid(inputs: KpiInputs, model: KpiResponse, viewModel: KpiViewModel) {
    ResponsiveColumns(
        items = inputs.formats.mapIndexed { index, fmt -> index to fmt },
        minColumnWidth = 330.dp,
        maxColumns = 3,
    ) { (index, fmt) ->
        val computed = model.formats.firstOrNull { it.cl == fmt.cl }
        FormatCardEditor(
            input = fmt,
            computed = computed,
            onSet = { key, v -> viewModel.setFormat(index, key, v) },
        )
    }
}

@Composable
private fun ChartsArea(model: KpiResponse, inputs: KpiInputs, modifier: Modifier = Modifier) {
    var tab by remember { mutableIntStateOf(0) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TabRow(selectedTabIndex = tab) {
            TABS.forEachIndexed { i, label ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
            }
        }
        when (tab) {
            0 -> Annee1Charts(model)
            1 -> SensibiliteCharts(model, inputs)
            2 -> MonthlyTable(model)
        }
    }
}

@Composable
private fun Annee1Charts(model: KpiResponse) {
    val months = model.months
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ChartCard(
            "Résultat mensuel et cumulé",
            legend = listOf("Résultat mensuel" to ChartBarPositive, "Résultat cumulé" to ChartLineTeal),
        ) {
            Chart(
                xLabels = months.map { it.mois },
                bars = listOf(SeriesSpec(months.map { it.resultat }, ChartBarPositive, signColored = true)),
                lines = listOf(SeriesSpec(months.map { it.cumul }, ChartLineTeal)),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        ChartCard(
            "Trésorerie cumulée",
            legend = listOf("Trésorerie" to ChartAreaAmber),
        ) {
            Chart(
                xLabels = months.map { it.mois },
                bars = emptyList(),
                lines = listOf(SeriesSpec(months.map { it.treso }, ChartAreaAmber, area = true)),
                zeroColor = ChartZeroRed,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@Composable
private fun SensibiliteCharts(model: KpiResponse, inputs: KpiInputs) {
    val sensPate = model.sensibilite.pate
    val sensMix = model.sensibilite.mix
    // Index de l'hypothèse courante sur la courbe de sensibilité pâte (pas de 0,05 €/kg)
    val refIndex = sensPate.indexOfFirst { abs(it.pate - inputs.pate) < 0.026 }.takeIf { it >= 0 }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ChartCard(
            "Résultat année 1 selon le prix de la pâte de citron (1 à 2 €/kg, fret import en sus)",
            legend = listOf("Résultat année 1" to ChartLineSky),
        ) {
            Chart(
                xLabels = sensPate.map { fmtPate(it.pate) },
                bars = emptyList(),
                lines = listOf(SeriesSpec(sensPate.map { it.resultat }, ChartLineSky)),
                refIndex = refIndex,
                refLabel = "Hypothèse actuelle",
                zeroColor = ChartZeroRed,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        ChartCard(
            "Résultat année 1 selon le mix CHR / GMS (tous formats cumulés)",
            legend = listOf("Résultat année 1" to ChartBarPositive),
        ) {
            Chart(
                xLabels = sensMix.map { fmtMix(it.mix) },
                bars = listOf(SeriesSpec(sensMix.map { it.resultat }, ChartBarPositive, signColored = true)),
                lines = emptyList(),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

/** Grille responsive par lignes de colonnes (BoxWithConstraints, non lazy car scroll global). */
@Composable
private fun <T> ResponsiveColumns(
    items: List<T>,
    minColumnWidth: Dp,
    maxColumns: Int = 4,
    itemContent: @Composable (T) -> Unit,
) {
    BoxWithConstraints {
        val cols = (maxWidth / minColumnWidth).toInt().coerceIn(1, maxColumns)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.chunked(cols).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { item ->
                        Box(Modifier.weight(1f)) { itemContent(item) }
                    }
                    repeat(cols - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
