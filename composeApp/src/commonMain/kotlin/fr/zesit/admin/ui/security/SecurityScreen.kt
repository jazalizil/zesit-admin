package fr.zesit.admin.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.zesit.admin.security.SecurityFinding
import fr.zesit.admin.security.SecurityImage
import fr.zesit.admin.ui.common.OutlinedButtonVariant
import fr.zesit.admin.ui.common.SecurityAuthPanel
import fr.zesit.admin.ui.common.StatePanel
import fr.zesit.admin.ui.common.VariantOutlinedButton
import fr.zesit.admin.ui.theme.fieldColors
import fr.zesit.admin.viewmodel.SecurityUiState
import fr.zesit.admin.viewmodel.SecurityViewModel

/**
 * Section « Sécurité » — analyse de vulnérabilités des images des conteneurs
 * du VPS (zesit-vps/security-scanner, /sc-api — VPN-only) : santé du scanner,
 * totaux par sévérité, fiche par image (deltas du dernier scan), détail des
 * CVE par image, scan manuel et historique des cycles.
 *
 * L'authentification vit ici (état non connecté) ; une fois chargée, le bandeau
 * expose « Relancer un scan » et Déconnexion.
 *
 * Composant « expect » : l'agencement par plateforme vit dans l'`actual`
 * (desktop : desktopMain) ; l'agencement commun de référence est
 * [DefaultSecurityScreen]. Pattern de [fr.zesit.admin.ui.traffic.TrafficScreen].
 */
@Composable
expect fun SecurityScreen(
    viewModel: SecurityViewModel,
    modifier: Modifier = Modifier,
)

@Composable
internal fun DefaultSecurityScreen(
    viewModel: SecurityViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    when {
        // Serveur non configuré : panneau de connexion sur la page même.
        !state.configured -> SecurityAuthPanel(
            baseUrl = viewModel.env.securityBaseUrlDefault,
            tokenAvailable = viewModel.hasSecretToken(),
            error = state.error,
            onConnect = viewModel::connect,
            modifier = modifier,
        )
        // Configuration présente mais rien de chargé : erreur ou chargement.
        state.summary == null -> when {
            state.error != null -> SecurityErrorState(state.error, viewModel, modifier)
            else -> StatePanel(modifier) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Analyse des vulnérabilités…", style = MaterialTheme.typography.bodyMedium)
            }
        }
        else -> SecurityDashboard(state, viewModel, modifier)
    }
}

/** Erreur au chargement : message + Réessayer + Déconnexion. */
@Composable
private fun SecurityErrorState(error: String?, viewModel: SecurityViewModel, modifier: Modifier) {
    StatePanel(modifier) {
        Icon(
            Icons.Default.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            error ?: "Connexion à security-scanner impossible.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.Button(onClick = viewModel::reload) {
                Text("Réessayer")
            }
            VariantOutlinedButton(onClick = viewModel::disconnect, variant = OutlinedButtonVariant.DANGER) {
                Text("Déconnexion")
            }
        }
    }
}

@Composable
private fun SecurityDashboard(state: SecurityUiState, viewModel: SecurityViewModel, modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 1180.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HealthBar(state, viewModel)
                TotalsCards(state)
                ImagesCard(state, viewModel)
                state.scans?.let { ScansCard(it) }
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

    // Fiche détaillée d'une image (dialogue)
    val detail = state.detail
    if (detail != null || state.detailLoading) {
        FindingsDialog(state, viewModel)
    }
}

// --- Bandeau santé ---

@Composable
private fun HealthBar(state: SecurityUiState, viewModel: SecurityViewModel) {
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
                Icon(
                    Icons.Default.BugReport,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.weight(1f)) {
                val health = state.health
                if (health != null) {
                    val scanning = health.scanning || state.scanRequested
                    Text(
                        if (scanning) "Scan des images en cours…" else "Service OK — API /sc-api active",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        buildString {
                            health.trivy_version?.let { append("Trivy $it · ") }
                            append(health.severities.joinToString("/").ifBlank { "toutes sévérités" })
                            append(" · MAJ ${state.summary?.generated_at?.take(16)?.replace('T', ' ') ?: "—"}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            VariantOutlinedButton(
                onClick = viewModel::triggerScan,
                enabled = !state.scanRequested,
                variant = OutlinedButtonVariant.ACCENT,
            ) {
                if (state.scanRequested) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text("Relancer un scan")
            }
            VariantOutlinedButton(onClick = viewModel::disconnect, variant = OutlinedButtonVariant.ACCENT) {
                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// --- Cartes totaux ---

@Composable
private fun TotalsCards(state: SecurityUiState) {
    val summary = state.summary ?: return
    val totals = summary.totals
    val imagesError = summary.images.count { it.status == "error" }
    val cards = listOf(
        Triple("Vulnérabilités", summary.total, "Toutes les images scannées"),
        Triple("Critiques", totals["CRITICAL"] ?: 0, "À corriger en priorité"),
        Triple("Hautes", totals["HIGH"] ?: 0, "Risque élevé"),
        Triple("Images suivies", summary.images.size, if (imagesError > 0) "$imagesError en échec de scan" else "Aucune erreur de scan"),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.forEach { (label, value, caption) ->
            OutlinedCard(Modifier.weight(1f)) {
                Column(Modifier.padding(14.dp)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        fmtInt(value),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (label == "Critiques" && value > 0) {
                            severityColor("CRITICAL")
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Text(
                        caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// --- Images ---

@Composable
private fun ImagesCard(state: SecurityUiState, viewModel: SecurityViewModel) {
    val summary = state.summary ?: return
    OutlinedCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Images des conteneurs — ${summary.images.size}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (summary.images.isEmpty()) {
                Text(
                    "Aucun scan encore enregistré (le premier cycle démarre au boot du service).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            summary.images.forEach { img -> ImageRow(img, viewModel) }
        }
    }
}

@Composable
private fun ImageRow(img: SecurityImage, viewModel: SecurityViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.openDetail(img.image_id) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Pastille de sévérité (point coloré + count total)
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(severityColor(img.severity_top).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                img.severity_top?.let { sevShort(it) } ?: "OK",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (img.severity_top != null) severityColor(img.severity_top) else MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    img.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!img.active) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "inactive",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (img.status == "error") {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "ÉCHEC SCAN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(
                img.ref,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            // Comptes par sévérité (dans l'ordre usuel)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                img.counts.entries
                    .sortedBy { orderOf(it.key) }
                    .forEach { (sev, n) ->
                        Text(
                            "${sevShort(sev)} $n",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (n > 0) severityColor(sev) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
            }
            // Deltas du dernier scan : nouvelles / corrigées
            if (!img.first_scan && (img.new.isNotEmpty() || img.fixed.isNotEmpty())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (img.new.isNotEmpty()) {
                        Text(
                            "+${img.new.size} ${if (img.new.size > 1) "nouvelles" else "nouvelle"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = severityColor(
                                if (img.new.any { it.severity == "CRITICAL" }) "CRITICAL" else "HIGH",
                            ),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (img.fixed.isNotEmpty()) {
                        Text(
                            "${img.fixed.size} corrigée" + if (img.fixed.size > 1) "s" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

// --- Fiche détaillée (dialogue) ---

@Composable
private fun FindingsDialog(state: SecurityUiState, viewModel: SecurityViewModel) {
    val detail = state.detail
    AlertDialog(
        onDismissRequest = viewModel::closeDetail,
        title = {
            Text(
                if (detail != null) "Vulnérabilités — " + detail.image.name else "Chargement de la fiche…",
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (detail == null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                } else {
                    // Filtres par sévérité (présentes dans l'image)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = state.detailSeverity == null,
                            onClick = { viewModel.setDetailSeverity(null) },
                            label = { Text("Toutes (${fmtInt(detail.total)})") },
                        )
                        (detail.image.counts.keys + listOf("CRITICAL", "HIGH", "MEDIUM", "LOW"))
                            .distinct()
                            .filter { (detail.image.counts[it] ?: 0) > 0 }
                            .sortedBy(::orderOf)
                            .forEach { sev ->
                                FilterChip(
                                    selected = state.detailSeverity == sev,
                                    onClick = { viewModel.setDetailSeverity(sev) },
                                    label = { Text("${sevShort(sev)} ${detail.image.counts[sev]}") },
                                )
                            }
                    }
                    OutlinedTextField(
                        value = state.detailSearch,
                        onValueChange = viewModel::setDetailSearch,
                        placeholder = { Text("Rechercher (CVE, paquet…)") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                    )
                    if (state.detailLoading) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    }
                    Text(
                        "${fmtInt(detail.findings.size)} / ${fmtInt(detail.total)} vulnérabilités",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    detail.findings.forEach { f -> FindingRow(f) }
                    if (detail.findings.size < detail.total) {
                        TextButton(onClick = viewModel::loadMoreDetail, enabled = !state.detailLoadingMore) {
                            if (state.detailLoadingMore) {
                                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Charger plus")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::closeDetail) { Text("Fermer") } },
    )
}

@Composable
private fun FindingRow(f: SecurityFinding) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                sevShort(f.severity),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = severityColor(f.severity),
                modifier = Modifier.width(44.dp),
            )
            Text(
                f.cve_id,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "→ fix ${f.fixed ?: "?"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row {
            Text(
                listOfNotNull(
                    f.`package`?.let { pkg -> if (f.installed != null) "$pkg ${f.installed}" else pkg },
                    f.title?.take(60),
                ).joinToString(" — "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// --- Historique des scans ---

@Composable
private fun ScansCard(scans: fr.zesit.admin.security.SecurityScans) {
    if (scans.runs.isEmpty()) return
    OutlinedCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Historique des scans",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            scans.runs.take(8).forEach { run ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "#${run.id}",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(38.dp),
                    )
                    Text(
                        run.started.take(16).replace('T', ' '),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(118.dp),
                    )
                    Text(
                        when (run.trigger) {
                            "startup" -> "boot"
                            "manual" -> "manuel"
                            else -> "planifié"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(52.dp),
                    )
                    Text(
                        listOfNotNull(
                            run.images_ok?.let { "$it images" },
                            run.images_error?.takeIf { it > 0 }?.let { "$it échecs" },
                        ).joinToString(" · ") + if (run.status == "error") " · ERREUR" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (run.status == "error") {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// --- Helpers ---

/** Libellé court par sévérité : CRITICAL -> CRIT, HIGH -> HIGH, MEDIUM -> MED, LOW -> LOW. */
private fun sevShort(severity: String): String = when (severity.uppercase()) {
    "CRITICAL" -> "CRIT"
    "HIGH" -> "HIGH"
    "MEDIUM" -> "MED"
    "LOW" -> "LOW"
    else -> "?"
}

/** Ordre de tri des sévérités (CRITICAL < HIGH < MEDIUM < LOW). */
private fun orderOf(severity: String): Int = when (severity.uppercase()) {
    "CRITICAL" -> 0
    "HIGH" -> 1
    "MEDIUM" -> 2
    "LOW" -> 3
    else -> 4
}

/** Couleur par sévérité (les basses restent neutres). */
@Composable
private fun severityColor(severity: String?): Color = when (severity?.uppercase()) {
    "CRITICAL" -> MaterialTheme.colorScheme.error
    "HIGH" -> Color(0xFFE65100)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

internal fun fmtInt(n: Int): String = fr.zesit.admin.ui.kpi.fmtInt(n.toDouble())
