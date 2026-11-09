package fr.zesit.admin.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.zesit.admin.data.ThemeMode
import fr.zesit.admin.viewmodel.NotificationsViewModel

/**
 * Section Réglages — « expect » : l'agencement par plateforme vit dans
 * l'`actual` (desktop : desktopMain) ; l'agencement commun de référence est
 * [DefaultSettingsScreen].
 */
@Composable
expect fun SettingsScreen(
    viewModel: NotificationsViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
)

/**
 * Réglages de l'app — liste à texte (gauche) / valeur (droite) :
 *  - Apparence : cycle Système → Clair → Sombre (appliqué immédiatement)
 *  - Réinitialiser les données : efface credentials + session + caches
 *    (confirmation)
 *  - Version
 * Les identifiants ntfy / KPI se saisissent désormais depuis les pages du
 * dashboard (panneau de connexion en état non connecté).
 */
@Composable
internal fun DefaultSettingsScreen(
    viewModel: NotificationsViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val themeMode by viewModel.themeMode.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 480.dp)) {
                SettingsRow(
                    label = "Apparence",
                    value = themeMode.label,
                    onClick = { viewModel.setTheme(nextThemeMode(themeMode)) },
                )
                HorizontalDivider()
                SettingsRow(
                    label = "Réinitialiser les données",
                    value = "Réinitialiser",
                    valueColor = MaterialTheme.colorScheme.error,
                    onClick = { showResetDialog = true },
                )
                HorizontalDivider()
                SettingsRow(label = "Version", value = "1.0.0")
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Réinitialiser les données ?") },
            text = {
                Text(
                    "Efface les identifiants ntfy et KPI, la session, l'historique des " +
                        "notifications et les hypothèses KPI. L'apparence est conservée. " +
                        "Les pages du dashboard repasseront en mode connexion.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetData()
                    showResetDialog = false
                }) {
                    Text("Réinitialiser", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Annuler") }
            },
        )
    }
}

/** Cycle d'apparence : Système → Clair → Sombre → Système. */
private fun nextThemeMode(mode: ThemeMode): ThemeMode =
    ThemeMode.entries[(mode.ordinal + 1) % ThemeMode.entries.size]

/** Ligne de réglage : libellé à gauche, valeur (ou action) à droite. */
@Composable
private fun SettingsRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 18.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 18.dp)
    }
    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = valueColor,
        )
    }
}
