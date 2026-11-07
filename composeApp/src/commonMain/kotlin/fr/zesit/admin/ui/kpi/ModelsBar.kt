package fr.zesit.admin.ui.kpi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.zesit.admin.viewmodel.KpiUiState

/**
 * Barre de gestion des modèles enregistrés (web : composant ModelsBar) :
 * sélecteur, Charger, Enregistrer/Réenregistrer, Supprimer.
 *
 * Composant « expect » : l'agencement par plateforme vit dans l'`actual`
 * (desktop : desktopMain) ; le commun de référence est [DefaultModelsBar].
 */
@Composable
expect fun ModelsBar(
    state: KpiUiState,
    onSelect: (String?) -> Unit,
    onLoad: () -> Unit,
    onSave: (name: String, description: String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
)

@Composable
internal fun DefaultModelsBar(
    state: KpiUiState,
    onSelect: (String?) -> Unit,
    onLoad: () -> Unit,
    onSave: (name: String, description: String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var saveDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val selected = state.models.firstOrNull { it.id == state.selectedModelId }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            OutlinedButton(onClick = { menuOpen = true }, enabled = state.models.isNotEmpty() || state.modelsLoading) {
                Text(
                    if (selected != null) {
                        "${selected.name} · MAJ ${fmtIsoDate(selected.updatedAt)} · rés. ${eur(selected.kpi.resultatAnnee1)}"
                    } else {
                        "— Modèles enregistrés (${state.models.size}) —"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                state.models.forEach { m ->
                    DropdownMenuItem(
                        text = {
                            Text("${m.name} · MAJ ${fmtIsoDate(m.updatedAt)} · rés. ${eur(m.kpi.resultatAnnee1)}")
                        },
                        onClick = {
                            onSelect(m.id)
                            menuOpen = false
                        },
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onLoad,
            enabled = state.selectedModelId != null && !state.modelBusy,
        ) { Text("Charger") }

        OutlinedButton(
            onClick = {
                if (selected != null) {
                    onSave(selected.name, selected.description)
                } else {
                    name = ""
                    description = ""
                    saveDialog = true
                }
            },
            enabled = !state.modelBusy,
        ) { Text(if (selected != null) "Réenregistrer" else "Enregistrer") }

        OutlinedButton(
            onClick = { deleteDialog = true },
            enabled = state.selectedModelId != null && !state.modelBusy,
        ) { Text("Supprimer") }

        if (state.modelsLoading) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        }
    }

    if (saveDialog) {
        AlertDialog(
            onDismissRequest = { saveDialog = false },
            title = { Text("Enregistrer le scénario") },
            text = {
                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nom") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optionnel)") },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isNotEmpty()) {
                            saveDialog = false
                            onSave(trimmed, description.trim())
                        }
                    },
                ) { Text("Enregistrer") }
            },
            dismissButton = {
                TextButton(onClick = { saveDialog = false }) { Text("Annuler") }
            },
        )
    }

    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("Supprimer le modèle") },
            text = { Text("Supprimer « ${selected?.name ?: ""} » ? Cette action est définitive.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteDialog = false
                        onDelete()
                    },
                ) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialog = false }) { Text("Annuler") }
            },
        )
    }
}

/** "2025-11-30T…" -> "30/11/2025". */
fun fmtIsoDate(iso: String): String {
    val parts = iso.take(10).split('-')
    if (parts.size != 3) return iso
    return "${parts[2]}/${parts[1]}/${parts[0]}"
}
