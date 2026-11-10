package fr.zesit.admin.ui.traffic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.zesit.admin.traffic.TrafficRequest
import fr.zesit.admin.traffic.TrafficStatsEnvelope
import fr.zesit.admin.ui.common.OutlinedButtonVariant
import fr.zesit.admin.ui.common.StatePanel
import fr.zesit.admin.ui.common.TrafficAuthPanel
import fr.zesit.admin.ui.common.VariantOutlinedButton
import fr.zesit.admin.ui.kpi.Chart
import fr.zesit.admin.ui.kpi.ChartAreaAmber
import fr.zesit.admin.ui.kpi.ChartCard
import fr.zesit.admin.ui.kpi.ChartZeroSlate
import fr.zesit.admin.ui.kpi.SeriesSpec
import fr.zesit.admin.ui.theme.filledButtonColors
import fr.zesit.admin.ui.theme.fieldColors
import fr.zesit.admin.viewmodel.TrafficUiState
import fr.zesit.admin.viewmodel.TrafficViewModel

/**
 * Section « Traffic » — dashboard d'analyse des données traffic-inspector
 * (zesit-vps/traffic-inspector, /ti-api — VPN-only) : santé du service,
 * KPIs, timeline, tops, alertes suspectes et journal filtrable, fiche IP.
 *
 * L'authentification vit ici (état non connecté) ; une fois chargée, le
 * bandeau expose un bouton Déconnexion (power).
 *
 * Composant « expect » : l'agencement par plateforme vit dans l'`actual`
 * (desktop : desktopMain) ; l'agencement commun de référence est
 * [DefaultTrafficScreen].
 */
@Composable
expect fun TrafficScreen(
    viewModel: TrafficViewModel,
    modifier: Modifier = Modifier,
)

@Composable
internal fun DefaultTrafficScreen(
    viewModel: TrafficViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    when {
        // Serveur non configuré : panneau de connexion sur la page même.
        !state.configured -> TrafficAuthPanel(
            baseUrl = viewModel.env.trafficBaseUrlDefault,
            tokenAvailable = viewModel.hasSecretToken(),
            error = state.error,
            onConnect = viewModel::connect,
            modifier = modifier,
        )
        // Configuration présente mais rien de chargé : erreur ou chargement.
        state.stats == null -> when {
            state.error != null -> TrafficErrorState(state.error, viewModel, modifier)
            else -> StatePanel(modifier) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Analyse du trafic…", style = MaterialTheme.typography.bodyMedium)
            }
        }
        else -> TrafficDashboard(state, viewModel, modifier)
    }
}

/** Erreur au chargement : message + Réessayer + Déconnexion. */
@Composable
private fun TrafficErrorState(error: String?, viewModel: TrafficViewModel, modifier: Modifier) {
    StatePanel(modifier) {
        Icon(
            Icons.Default.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            error ?: "Connexion à traffic-inspector impossible.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::reloadAll, colors = filledButtonColors()) {
                Text("Réessayer")
            }
            VariantOutlinedButton(onClick = viewModel::disconnect, variant = OutlinedButtonVariant.DANGER) {
                Text("Déconnexion")
            }
        }
    }
}

@Composable
private fun TrafficDashboard(state: TrafficUiState, viewModel: TrafficViewModel, modifier: Modifier) {
    val scroll = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 1180.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HealthBar(state, viewModel)
                PeriodBar(state, viewModel)
                if (state.stats != null) {
                    StatCards(state)
                    TimelineCard(state.stats)
                    TopsGrid(state, viewModel)
                    state.alerts?.let { AlertsCard(it) }
                    JournalCard(state, viewModel)
                }
                state.error?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    // Fiche IP (dialogue)
    if (state.ipLoading) {
        AlertDialog(
            onDismissRequest = viewModel::closeIp,
            title = { Text("Chargement de la fiche IP…") },
            text = { CircularProgressIndicator() },
            confirmButton = { TextButton(onClick = viewModel::closeIp) { Text("Fermer") } },
        )
    }
    state.ipSheet?.let { sheet ->
        IpSheetDialog(sheet, viewModel)
    }
}

// --- Bandeau santé ---

@Composable
private fun HealthBar(state: TrafficUiState, viewModel: TrafficViewModel) {
    OutlinedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = viewModel::reloadAll) {
                    Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir")
                }
            }
            Column(Modifier.weight(1f)) {
                val health = state.health
                if (health != null) {
                    Text(
                        if (health.api_enabled) "Service OK — API /ti-api active" else "API inactive",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        buildString {
                            append("geoip : ")
                            append(if (health.geoip_local) "local" else "absent")
                            append(" · secrets : ")
                            append(if (health.secrets_captured) "capturés" else "non captés")
                            append(" · caddy : ")
                            append(if (health.caddy_ingest) "ingéré" else "non ingéré")
                            state.stats?.generated_at?.let { append(" · MAJ $it") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            VariantOutlinedButton(onClick = viewModel::disconnect, variant = OutlinedButtonVariant.ACCENT) {
                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Déconnexion")
            }
        }
    }
}

// --- Période ---

private val PERIODS = listOf(
    24.0 to "24 h",
    168.0 to "7 jours",
    0.0 to "Tout",
)

@Composable
private fun PeriodBar(state: TrafficUiState, viewModel: TrafficViewModel) {
    OutlinedCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Période :", style = MaterialTheme.typography.bodyMedium)
                PERIODS.forEach { (hours, label) ->
                    FilterChip(
                        selected = state.hours == hours,
                        onClick = { viewModel.setHours(hours) },
                        label = { Text(label) },
                    )
                }
            }
        }
    }
}

// --- Cartes stats ---

@Composable
private fun StatCards(state: TrafficUiState) {
    val stats = state.stats?.stats ?: return
    val cards = listOf(
        Triple("Requêtes", fmtInt(stats.total), "IP uniques : ${fmtInt(stats.unique_ips)}"),
        Triple("Suspectes", fmtInt(stats.suspicious), "Sur la période"),
        Triple(
            "Top IP",
            stats.top_ips.firstOrNull()?.ip ?: "—",
            stats.top_ips.firstOrNull()?.let { "${fmtInt(it.c)} requêtes" } ?: "Aucune donnée",
        ),
        Triple(
            "Top chemin",
            stats.top_paths.firstOrNull()?.path ?: "—",
            stats.top_paths.firstOrNull()?.let { "${fmtInt(it.c)} requêtes" } ?: "Aucune donnée",
        ),
    )
    // Grille responsive : 4 cartes desktop, empilées sur mobile.
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.chunked(4).forEach { rows ->
            val used = rows
            if (used.size == 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    used.forEach { card ->
                        OutlinedCard(Modifier.weight(1f)) {
                            Column(Modifier.padding(14.dp)) {
                                Text(card.first, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    card.second,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    card.third,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            } else {
                used.forEach { card ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(card.first, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(card.second, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text(card.third, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// --- Timeline ---

@Composable
private fun TimelineCard(stats: TrafficStatsEnvelope) {
    val timeline = stats.stats.timeline
    if (timeline.isEmpty()) return
    // Sous-échantillonne si trop de points (24h = 24 barres ; 7j = 56 ; tout = plafonné).
    val bucket = ((timeline.size - 1) / 56 + 1).coerceAtLeast(1)
    val bars = timeline.chunked(bucket).map { chunk -> chunk.sumOf { it.c }.toDouble() }
    val xLabels = timeline.chunked(bucket).map { chunk ->
        chunk.firstOrNull()?.h?.substringAfter("T")?.padStart(2, '0') ?: ""
    }
    ChartCard(
        title = "Requêtes par ${if (bucket == 1) "heure" else "bloc de ${bucket}h"}",
        legend = listOf("Requêtes" to ChartAreaAmber),
    ) {
        Chart(
            xLabels = xLabels,
            bars = listOf(SeriesSpec(values = bars, color = ChartAreaAmber)),
            lines = emptyList(),
            zeroColor = ChartZeroSlate,
            height = 220.dp,
        )
    }
}

// --- Tops ---

@Composable
private fun TopsGrid(state: TrafficUiState, viewModel: TrafficViewModel) {
    val stats = state.stats?.stats ?: return
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TopListCard(
            title = "Top IPs",
            rows = stats.top_ips.take(8).map { it.ip to it.c },
            modifier = Modifier.weight(1f),
            onClick = { viewModel.openIp(it) },
        )
        TopListCard(
            title = "Top chemins",
            rows = stats.top_paths.take(8).map { it.path to it.c },
            modifier = Modifier.weight(1f),
            onClick = { viewModel.setSearch(it) },
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TopListCard(
            title = "Top hôtes",
            rows = stats.top_hosts.take(8).map { it.host to it.c },
            modifier = Modifier.weight(1f),
        )
        TopListCard(
            title = "Top user-agents",
            rows = stats.top_uas.take(6).map { (it.ua.ifBlank { "?" }) to it.c },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TopListCard(
    title: String,
    rows: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    onClick: ((String) -> Unit)? = null,
) {
    OutlinedCard(modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (rows.isEmpty()) {
                Text("Aucune donnée", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            rows.forEach { (label, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (onClick != null) Modifier.clickable { onClick(label) } else Modifier)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        fmtInt(count),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// --- Alertes ---

@Composable
private fun AlertsCard(alerts: fr.zesit.admin.traffic.TrafficAlerts) {
    if (alerts.requests.isEmpty()) return
    OutlinedCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Alertes suspectes — ${fmtInt(alerts.total)} sur la période",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
            }
            alerts.requests.take(12).forEach { req ->
                RequestRow(req)
            }
        }
    }
}

// --- Journal ---

@Composable
private fun JournalCard(state: TrafficUiState, viewModel: TrafficViewModel) {
    OutlinedCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Journal des requêtes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.search,
                    onValueChange = viewModel::setSearch,
                    placeholder = { Text("Rechercher (path, IP, UA…)") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    colors = fieldColors(),
                )
                if (state.journalLoading) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.suspiciousOnly,
                    onClick = { viewModel.setSuspiciousOnly(!state.suspiciousOnly) },
                    label = { Text("Suspectes") },
                )
                FilterChip(
                    selected = state.source == null,
                    onClick = { viewModel.setSource(null) },
                    label = { Text("Toutes sources") },
                )
                FilterChip(
                    selected = state.source == "honeypot",
                    onClick = { viewModel.setSource("honeypot") },
                    label = { Text("honeypot") },
                )
                FilterChip(
                    selected = state.source == "caddy",
                    onClick = { viewModel.setSource("caddy") },
                    label = { Text("caddy") },
                )
            }
            state.journal?.let { journal ->
                Text(
                    "${fmtInt(journal.requests.size)} / ${fmtInt(journal.total)} requêtes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                journal.requests.forEach { req ->
                    RequestRow(req)
                }
                if (journal.requests.size < journal.total) {
                    VariantOutlinedButton(
                        onClick = viewModel::loadMore,
                        enabled = !state.loadingMore,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        if (state.loadingMore) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Charger plus")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestRow(req: TrafficRequest) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                req.method,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(48.dp),
            )
            Text(
                req.path + (req.query?.let { "?$it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                )
            Text(
                listOfNotNull(
                    req.status?.toString(),
                    req.source,
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row {
            Text(
                listOfNotNull(
                    req.ts.take(19),
                    req.ip,
                    req.country_code,
                    req.ua_browser?.take(20),
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (req.suspicious > 0 || req.reason != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "⚠ ${req.reason ?: "suspecte"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDivider(Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// --- Fiche IP ---

@Composable
private fun IpSheetDialog(sheet: fr.zesit.admin.traffic.TrafficIpSheet, viewModel: TrafficViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::closeIp,
        title = { Text("Fiche IP — ${sheet.ip}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val intel = sheet.intel
                Text(
                    listOfNotNull(
                        intel.country?.let { c -> if (intel.city != null) "$c — ${intel.city}" else c },
                        intel.as_org?.let { "AS${intel.asn ?: "?"} $it" },
                        intel.ptr,
                        intel.isp?.let { "FAI : $it" },
                        intel.abuse_confidence?.let { "AbuseIPDB : $it% (${fmtInt(intel.abuse_reports ?: 0)} signalements)" },
                        intel.abuse_usage,
                        intel.abuse_emails.firstOrNull()?.let { "signaler : $it" },
                    ).joinToString("\n"),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "${fmtInt(sheet.total)} requêtes de cette IP",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                sheet.requests.take(5).forEach { req ->
                    Text(
                        "${req.method} ${req.path.take(40)} · ${req.ts.take(19)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::closeIp) { Text("Fermer") } },
    )
}

private fun fmtInt(n: Int): String = fr.zesit.admin.ui.kpi.fmtInt(n.toDouble())
