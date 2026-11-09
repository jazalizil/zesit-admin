package fr.zesit.admin.ui.realforecast

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.zesit.admin.kpi.MedusaChannel
import fr.zesit.admin.kpi.MedusaComparison
import fr.zesit.admin.kpi.MedusaStatus
import fr.zesit.admin.kpi.MedusaVariant
import fr.zesit.admin.ui.common.KpiAuthPanel
import fr.zesit.admin.ui.kpi.Chart
import fr.zesit.admin.ui.kpi.ChartAreaAmber
import fr.zesit.admin.ui.kpi.ChartCard
import fr.zesit.admin.ui.kpi.ChartLineSky
import fr.zesit.admin.ui.kpi.ChartZeroSlate
import fr.zesit.admin.ui.kpi.KpiCard
import fr.zesit.admin.ui.kpi.KpiTone
import fr.zesit.admin.ui.kpi.SeriesSpec
import fr.zesit.admin.ui.kpi.fmtDec1
import fr.zesit.admin.ui.kpi.fmtInt
import fr.zesit.admin.viewmodel.MedusaUiState
import fr.zesit.admin.viewmodel.MedusaViewModel
import kotlin.math.abs
import fr.zesit.admin.ui.theme.fieldColors

/**
 * Section « Réel / Prévisionnel » — portage Compose de RealVsForecast.tsx
 * (zesit-kpi/apps/web) : ventes Medusa synchronisées (1×/jour côté serveur)
 * comparées au prévisionnel du modèle chargé dans la section KPI.
 *
 * L'authentification au serveur KPI vit ici (état non connecté, même session
 * que la page KPI) : le panneau de connexion remplace l'ancien renvoi vers les
 * Réglages ; une fois connecté, la barre de période expose une Déconnexion
 * (power_off).
 *
 * Composant « expect » : l'agencement par plateforme vit dans l'`actual`
 * (desktop : desktopMain) ; l'agencement commun de référence est
 * [DefaultRealForecastScreen].
 */
@Composable
expect fun RealForecastScreen(
    viewModel: MedusaViewModel,
    modifier: Modifier = Modifier,
)

@Composable
internal fun DefaultRealForecastScreen(
    viewModel: MedusaViewModel,
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
        else -> Content(state, viewModel, modifier)
    }
}

// --- Contenu ---

private val AlertAmberBg = Color(0xFFFFFBEB)
private val AlertAmberBorder = Color(0xFFFCD34D)
private val AlertAmberText = Color(0xFF92400E)
private val AlertRedBg = Color(0xFFFEF2F2)
private val AlertRedBorder = Color(0xFFFECACA)
private val AlertRedText = Color(0xFF991B1B)
private val GoodEmerald = Color(0xFF059669)
private val BadRed = Color(0xFFDC2626)
private val BarPrevu = ChartZeroSlate

private val PRESETS = listOf(
    "7" to "7 j",
    "30" to "30 j",
    "90" to "90 j",
    "month" to "Mois en cours",
)

@Composable
private fun Content(state: MedusaUiState, viewModel: MedusaViewModel, modifier: Modifier) {
    var showMapping by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 1180.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PeriodBar(state, viewModel, showMapping) { showMapping = !showMapping }
                state.comparison?.let { comparison ->
                    JoursManquantsAlert(comparison)
                    UnmappedAlert(comparison)
                    GlobalCards(comparison, state.status)
                    ChartsGrid(comparison)
                    DetailTable(comparison)
                }
                if (showMapping) {
                    MappingCard(state, viewModel)
                }
                state.notice?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.comparisonError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// --- Barre de période + synchro (web : première Card) ---

@Composable
private fun PeriodBar(
    state: MedusaUiState,
    viewModel: MedusaViewModel,
    showMapping: Boolean,
    onToggleMapping: () -> Unit,
) {
    OutlinedCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Période :", style = MaterialTheme.typography.bodyMedium)
                PRESETS.forEach { (key, label) ->
                    FilterChip(
                        selected = state.preset == key,
                        onClick = { viewModel.setPreset(key) },
                        label = { Text(label) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ISODateField(value = state.from) { from -> viewModel.setPeriod(from, state.to) }
                Text("→", style = MaterialTheme.typography.bodyMedium)
                ISODateField(value = state.to) { to -> viewModel.setPeriod(state.from, to) }
                OutlinedButton(onClick = viewModel::syncNow, enabled = !state.syncing) {
                    if (state.syncing) {
                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Synchro…")
                    } else {
                        Text("Synchroniser maintenant")
                    }
                }
                OutlinedButton(onClick = onToggleMapping) {
                    Text(if (showMapping) "Masquer le mapping" else "Mapping variantes / canaux")
                }
                OutlinedButton(onClick = viewModel::disconnect) {
                    Icon(
                        Icons.Default.PowerOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Déconnexion")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.loading) ChipItem("chargement…")
                val last = state.status?.last
                when {
                    last != null && last.status == "ok" ->
                        ChipItem("Dernière synchro : ${last.run_date} (${last.orders_count} cmd)")
                    last != null -> ChipItem("Synchro ${last.run_date} en échec", alert = true)
                    else -> ChipItem("Aucune synchro pour l'instant")
                }
            }
        }
    }
}

/** Champ date ISO (YYYY-MM-DD) — ne propage que les dates valides. */
@Composable
private fun ISODateField(value: String, onValid: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw
            val candidate = raw.take(10)
            if (Regex("\\d{4}-\\d{2}-\\d{2}").matches(candidate) && candidate != value) onValid(candidate)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall,
        modifier = Modifier.width(130.dp),
        colors = fieldColors(),
    )
}

@Composable
private fun ChipItem(label: String, alert: Boolean = false) {
    OutlinedCard(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (alert) AlertRedBg else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (alert) AlertRedText else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

// --- Alertes ---

@Composable
private fun JoursManquantsAlert(comparison: MedusaComparison) {
    val nbJours = comparison.byDay.size
    val joursSync = comparison.sync.count { it.status == "ok" }
    val manquants = nbJours - joursSync
    if (manquants <= 0) return
    OutlinedCard(
        border = BorderStroke(1.dp, AlertAmberBorder),
        colors = CardDefaults.outlinedCardColors(containerColor = AlertAmberBg),
    ) {
        Text(
            "$manquants jour(s) sans synchro Medusa sur la période — le réel est incomplet. " +
                "Lancez « Synchroniser maintenant » ou vérifiez le service kpi-sync.",
            style = MaterialTheme.typography.bodyMedium,
            color = AlertAmberText,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun UnmappedAlert(comparison: MedusaComparison) {
    if (comparison.unmapped.isEmpty()) return
    val total = comparison.unmapped.sumOf { it.qty }
    val detail = comparison.unmapped.joinToString(", ") { u ->
        "${u.product_title ?: "?"} ${u.variant_title ?: u.variant_id} (${fmtInt(u.qty)})"
    }
    OutlinedCard(
        border = BorderStroke(1.dp, AlertRedBorder),
        colors = CardDefaults.outlinedCardColors(containerColor = AlertRedBg),
    ) {
        Text(
            "${fmtInt(total)} bouteille(s) vendue(s) sur des variantes non mappées ou des canaux " +
                "inconnus (exclues de la comparaison) : $detail → renseignez le mapping ci-dessous.",
            style = MaterialTheme.typography.bodyMedium,
            color = AlertRedText,
            modifier = Modifier.padding(12.dp),
        )
    }
}

// --- KPI cards (3) ---

@Composable
private fun GlobalCards(comparison: MedusaComparison, status: MedusaStatus?) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f)) {
            KpiCard(
                label = "Ventes réelles (mappées) — ${fmtDay(comparison.from)} → ${fmtDay(comparison.to)}",
                value = "${fmtInt(comparison.global.reel)} btl",
                sub = status?.let { "${fmtInt(it.totalQty.toDouble())} btl au total en base" },
                tone = KpiTone.NEUTRAL,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(Modifier.weight(1f)) {
            KpiCard(
                label = "Prévisionnel (modèle chargé) sur la même période",
                value = "${fmtInt(comparison.global.prevu)} btl",
                sub = "qty mensuelle × rampe du mois × (1 − casse) × part du canal",
                tone = KpiTone.NEUTRAL,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(Modifier.weight(1f)) {
            val pct = comparison.global.ecartPct
            KpiCard(
                label = "Écart réel − prévu",
                value = "${if (comparison.global.ecart >= 0) "+" else ""}${fmtInt(comparison.global.ecart)} btl",
                sub = pct?.let { "${if (it > 0) "+" else ""}${fmtDec1(it)} %" } ?: "—",
                tone = if ((pct ?: 0.0).let { abs(it) } <= 10) KpiTone.GOOD else KpiTone.BAD,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// --- Graphiques ---

@Composable
private fun ChartsGrid(comparison: MedusaComparison) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.weight(1f)) {
                ChartCard(
                    "Par format — prévu vs réel (bouteilles)",
                    legend = listOf("Prévu" to BarPrevu, "Réel" to ChartLineSky),
                ) {
                    Chart(
                        xLabels = comparison.byFormat.map { "${it.format} cl" },
                        bars = listOf(
                            SeriesSpec(comparison.byFormat.map { it.prevu }, BarPrevu),
                            SeriesSpec(comparison.byFormat.map { it.reel }, ChartLineSky),
                        ),
                        lines = emptyList(),
                        height = 260.dp,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
            Box(Modifier.weight(1f)) {
                ChartCard(
                    "Par canal de vente — prévu vs réel (bouteilles)",
                    legend = listOf("Prévu" to BarPrevu, "Réel" to ChartAreaAmber),
                ) {
                    Chart(
                        xLabels = comparison.byChannel.map { it.canal },
                        bars = listOf(
                            SeriesSpec(comparison.byChannel.map { it.prevu }, BarPrevu),
                            SeriesSpec(comparison.byChannel.map { it.reel }, ChartAreaAmber),
                        ),
                        lines = emptyList(),
                        height = 260.dp,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        }
        ChartCard(
            "Cumul jour par jour — réel vs prévu",
            legend = listOf("Cumul prévu" to BarPrevu, "Cumul réel" to ChartLineSky),
        ) {
            Chart(
                xLabels = comparison.byDay.map { fmtDay(it.day) },
                bars = emptyList(),
                lines = listOf(
                    SeriesSpec(comparison.byDay.map { it.cumulPrevu }, BarPrevu, dashed = true),
                    SeriesSpec(comparison.byDay.map { it.cumulReel }, ChartLineSky),
                ),
                zeroColor = BarPrevu,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

// --- Tableau détaillé par format × canal ---

private val TableColFormat = 80.dp
private val TableColNum = 84.dp
private val TableColPct = 80.dp

@Composable
private fun DetailTable(comparison: MedusaComparison) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(Modifier.padding(horizontal = 16.dp)) {
                Text("Format", style = headerStyle(), modifier = Modifier.width(TableColFormat))
                Text("Canal", style = headerStyle(), modifier = Modifier.weight(1f))
                Text("Prévu", style = headerStyle(), textAlign = TextAlign.End, modifier = Modifier.width(TableColNum))
                Text("Réel", style = headerStyle(), textAlign = TextAlign.End, modifier = Modifier.width(TableColNum))
                Text("Écart", style = headerStyle(), textAlign = TextAlign.End, modifier = Modifier.width(TableColNum))
                Text("Écart %", style = headerStyle(), textAlign = TextAlign.End, modifier = Modifier.width(TableColPct))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            comparison.byFormatChannel.forEach { r ->
                val ok = r.ecartPct?.let { abs(it) <= 10 }
                val ecartColor = when {
                    ok == null -> MaterialTheme.colorScheme.onSurface
                    ok -> GoodEmerald
                    else -> BadRed
                }
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${r.format} cl", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(TableColFormat))
                    Text(r.canal, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(fmtInt(r.prevu), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, modifier = Modifier.width(TableColNum))
                    Text(fmtInt(r.reel), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, modifier = Modifier.width(TableColNum))
                    Text(
                        "${if (r.ecart >= 0) "+" else ""}${fmtInt(r.ecart)}",
                        style = MaterialTheme.typography.bodySmall.copy(color = ecartColor),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(TableColNum),
                    )
                    Text(
                        r.ecartPct?.let { p -> "${if (p > 0) "+" else ""}${fmtDec1(p)} %" } ?: "—",
                        style = MaterialTheme.typography.bodySmall.copy(color = ecartColor),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(TableColPct),
                    )
                }
            }
            if (comparison.byFormatChannel.isEmpty()) {
                Text(
                    "Aucune donnée mappée sur la période.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun headerStyle() = MaterialTheme.typography.labelSmall

// --- Mapping variantes / canaux ---

@Composable
private fun MappingCard(state: MedusaUiState, viewModel: MedusaViewModel) {
    val mapping = state.mapping ?: return
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Mapping Medusa → prévisionnel", style = MaterialTheme.typography.titleMedium)

            Text("Variantes de produit → format", style = MaterialTheme.typography.titleSmall)
            Column {
                Row(Modifier.padding(bottom = 4.dp)) {
                    Text("Variante", style = headerStyle(), modifier = Modifier.weight(1f))
                    Text("SKU", style = headerStyle(), modifier = Modifier.width(110.dp))
                    Text("Format", style = headerStyle())
                }
                mapping.variants.forEach { v -> VariantRow(v, viewModel) }
                if (mapping.variants.isEmpty()) {
                    EmptyMapping("Aucune variante connue — lancez une synchro pour peupler le catalogue.")
                }
            }

            Text(
                "Sales channels → canal de vente (nom Medusa, associé par id — aucun mapping manuel)",
                style = MaterialTheme.typography.titleSmall,
            )
            Column {
                Row(Modifier.padding(bottom = 4.dp)) {
                    Text("Sales channel", style = headerStyle(), modifier = Modifier.weight(1f))
                    Text("Canal utilisé dans la comparaison", style = headerStyle())
                }
                mapping.channels.forEach { c ->
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text(
                            c.sales_channel_id,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(c.name ?: "—", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (mapping.channels.isEmpty()) {
                    EmptyMapping("Aucun sales channel connu — lancez une synchro pour peupler le catalogue.")
                }
            }
        }
    }
}

@Composable
private fun VariantRow(v: MedusaVariant, viewModel: MedusaViewModel) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            "${v.product_title ?: "?"} — ${v.variant_title ?: v.variant_id}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            v.sku ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp),
        )
        Box {
            OutlinedButton(onClick = { menuOpen = true }) {
                Text(v.format_cl?.let { "$it cl" } ?: "— non mappé —", style = MaterialTheme.typography.bodySmall)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("— non mappé —") },
                    onClick = {
                        menuOpen = false
                        viewModel.saveVariantFormat(v.variant_id, null)
                    },
                )
                listOf(25, 33, 50).forEach { cl ->
                    DropdownMenuItem(
                        text = { Text("$cl cl") },
                        onClick = {
                            menuOpen = false
                            viewModel.saveVariantFormat(v.variant_id, cl)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMapping(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    )
}

/** "2026-11-07" -> "07/11" (web : toLocaleDateString fr, 2 chiffres). */
private fun fmtDay(iso: String): String {
    val parts = iso.take(10).split('-')
    if (parts.size != 3) return iso
    return "${parts[2]}/${parts[1]}"
}
